package com.cryptoshield.order_service.service;

import com.cryptoshield.order_service.components.OrderMatchingEngine;
import com.cryptoshield.order_service.components.SymbolDemandProducer;
import com.cryptoshield.order_service.dto.request.ClosePositionRequest;
import com.cryptoshield.order_service.dto.response.ClosePositionResponse;
import com.cryptoshield.order_service.dto.response.OpenPositionResponse;
import com.cryptoshield.order_service.entity.OrderCondition;
import com.cryptoshield.order_service.enums.ErrorCode;
import com.cryptoshield.order_service.enums.OrderConditionStatus;
import com.cryptoshield.order_service.exception.AppException;
import com.cryptoshield.order_service.repository.OrderConditionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderExecutionService {
    private final WebClient.Builder webClientBuilder;
    private final OrderConditionRepository orderConditionRepository;
    private final SymbolDemandProducer symbolDemandProducer;
    private final OrderMatchingEngine orderMatchingEngine;
    private static final Duration CALL_TIMEOUT = Duration.ofSeconds(30);
    @Transactional
    public boolean execute(OrderCondition orderFromCache, BigDecimal executedPrice) {
        OrderCondition order = orderConditionRepository.findByIdForUpdate(orderFromCache.getId())
                .orElse(null);
        if (order == null || order.getStatus() != OrderConditionStatus.PENDING) {
            return true;
        }
        try {
            ClosePositionResponse res = closePosition(order, executedPrice);
            order.setStatus(OrderConditionStatus.EXECUTED);
            orderConditionRepository.save(order);
            log.info("Khớp lệnh {} - symbol={}, giá khớp={}, PnL={}",
                    order.getType(), order.getSymbol(), executedPrice, res.getRealizedPnl());

            boolean fullyClosed = res.getPosition() != null
                    && res.getPosition().getQuantity() != null
                    && res.getPosition().getQuantity().compareTo(BigDecimal.ZERO) == 0;
            if (fullyClosed) {
                cancelRemainingOrderConditions(order);
            }

        } catch (AppException e) {
            if (e.getErrorCode() == ErrorCode.POSITION_ALREADY_CLOSED) {
                order.setStatus(OrderConditionStatus.EXECUTED);
                orderConditionRepository.save(order);
                log.warn("Position đã đóng từ trước, đánh dấu {} là EXECUTED (idempotent)", order.getId());
            } else {
                order.setStatus(OrderConditionStatus.FAILED);
                orderConditionRepository.save(order);
                log.error("Khớp lệnh {} thất bại vĩnh viễn: {}", order.getId(), e.getErrorCode());
            }
        } catch (Exception e) {
            log.error("Lỗi tạm thời khi gọi wallet-service cho lệnh {}, giữ PENDING để retry: {}",
                    order.getId(), e.getMessage());
            return false;
        }

        symbolDemandProducer.releaseSymbol(order.getSymbol());
        return true;
    }
    private ClosePositionResponse closePosition(OrderCondition order, BigDecimal executedPrice) {
        ClosePositionRequest request = ClosePositionRequest.builder()
                .userId(order.getUserId())
                .positionId(order.getPositionId())
                .symbol(order.getSymbol())
                .currentPrice(executedPrice)
                .closedQuantity(order.getQuantity())
                .build();

        try {
            WebClient webClient = webClientBuilder.build();
            ClosePositionResponse response = webClient.post()
                    .uri("http://wallet-service/internal/position/close")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ClosePositionResponse.class)
                    .block(CALL_TIMEOUT);

            if (response == null || !response.isSuccess()) {
                throw new AppException(ErrorCode.WALLET_SERVICE_ERROR);
            }
            return response;

        } catch (WebClientResponseException.Conflict e) {
            throw new AppException(ErrorCode.POSITION_ALREADY_CLOSED);
        } catch (WebClientResponseException.NotFound e) {
            throw new AppException(ErrorCode.POSITION_NOT_FOUND);
        } catch (WebClientResponseException e) {
            String errorBody = e.getResponseBodyAsString();
            log.warn("Wallet service error [{}]: {}", e.getStatusCode(), errorBody);

            String walletMessage;
            try {
                OpenPositionResponse errorResponse = new ObjectMapper()
                        .readValue(errorBody, OpenPositionResponse.class);
                walletMessage = errorResponse.getMessage();
            } catch (Exception parseEx) {
                walletMessage = "Wallet service error";
            }

            throw new AppException(ErrorCode.WALLET_SERVICE_ERROR, walletMessage);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Không gọi được wallet-service khi đóng vị thế: {}", e.getMessage());
            throw new AppException(ErrorCode.WALLET_SERVICE_TIMEOUT);
        }
    }
    private void cancelRemainingOrderConditions(OrderCondition triggeredOrder) {
        List<OrderCondition> remaining = orderConditionRepository
                .findByPositionIdAndStatusAndIdNot(
                        triggeredOrder.getPositionId(),
                        OrderConditionStatus.PENDING,
                        triggeredOrder.getId()
                );

        for (OrderCondition other : remaining) {
            other.setStatus(OrderConditionStatus.CANCELLED);
            orderConditionRepository.save(other);

            orderMatchingEngine.unregister(other);
            symbolDemandProducer.releaseSymbol(other.getSymbol());

            log.info("Tự động hủy lệnh {} (type={}) do position {} đã đóng hết bởi lệnh {}",
                    other.getId(), other.getType(), other.getPositionId(), triggeredOrder.getId());
        }
    }
}