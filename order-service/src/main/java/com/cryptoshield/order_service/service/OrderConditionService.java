package com.cryptoshield.order_service.service;

import com.cryptoshield.order_service.components.OrderMatchingEngine;
import com.cryptoshield.order_service.components.PriceCache;
import com.cryptoshield.order_service.components.SymbolDemandProducer;
import com.cryptoshield.order_service.dto.request.CreateOrderConditionRequest;
import com.cryptoshield.order_service.dto.response.*;
import com.cryptoshield.order_service.entity.OrderCondition;
import com.cryptoshield.order_service.enums.*;
import com.cryptoshield.order_service.exception.AppException;
import com.cryptoshield.order_service.repository.OrderConditionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.internal.util.stereotypes.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import tools.jackson.databind.ObjectMapper;

import javax.swing.text.Position;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderConditionService {
    private final WebClient.Builder webClientBuilder;
    private final OrderConditionRepository orderConditionRepository;
    private final PriceCache priceCache;
    private final SymbolDemandProducer symbolDemandProducer;
    private final OrderMatchingEngine orderMatchingEngine;
    private static final Duration CALL_TIMEOUT = Duration.ofSeconds(30);
    @Transactional
    public OrderConditionResponse createOrderCondition(CreateOrderConditionRequest request) {
        PositionResponse position = fetchPosition(request.getPositionId());

        if (position.getStatus() != PositionStatus.OPEN) {
            throw new AppException(ErrorCode.POSITION_NOT_OPEN);
        }
        if (request.getQuantity().compareTo(position.getQuantity()) > 0) {
            throw new AppException(ErrorCode.QUANTITY_EXCEEDS_AVAILABLE);
        }
        validateTriggerPrice(position, request.getType(), request.getTriggerPrice());

        OrderSide side = switch (position.getSide()) {
            case "LONG" -> OrderSide.BUY;
            case "SHORT" -> OrderSide.SELL;
            default -> throw new AppException(ErrorCode.POSITION_SIDE_NOT_VALID);
        };
        OrderCondition condition = OrderCondition.builder()
                .userId(request.getUserId())
                .positionId(position.getPositionId())
                .symbol(position.getSymbol())
                .positionSide(side)
                .type(request.getType())
                .triggerPrice(request.getTriggerPrice())
                .quantity(request.getQuantity())
                .status(OrderConditionStatus.PENDING)
                .build();

        orderConditionRepository.save(condition);

        symbolDemandProducer.requestSymbol(condition.getSymbol());
        orderMatchingEngine.register(condition);

        return OrderConditionResponse.from(condition);
    }
    private PositionResponse fetchPosition(UUID positionId) {
        try {
            WebClient webClient = webClientBuilder.build();
            PositionResponse response = webClient.get()
                    .uri("http://wallet-service/internal/position/{id}", positionId)
                    .retrieve()
                    .bodyToMono(PositionResponse.class)
                    .block(CALL_TIMEOUT);

            if (response == null) {
                throw new AppException(ErrorCode.WALLET_SERVICE_ERROR);
            }
            return response;

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
        } catch (Exception e) {
            log.error("Không gọi được wallet-service: {}", e.getMessage());
            throw new AppException(ErrorCode.WALLET_SERVICE_TIMEOUT);
        }
    }
    private void validateTriggerPrice(PositionResponse position, OrderConditionType type, BigDecimal triggerPrice) {
        PriceResponse response;
        try {
            WebClient webClient = webClientBuilder.build();

            response = webClient.get()
                    .uri("http://market-data-service/api/price/{symbol}", position.getSymbol())
                    .retrieve()
                    .bodyToMono(PriceResponse.class)
                    .block(CALL_TIMEOUT);
        } catch (WebClientException e) {
            log.warn("Error call api :",e);
            throw new AppException(ErrorCode.MARKET_DATA_UNAVAILABLE);

        }

        if (response == null) {
            log.warn("Request null");
            throw new AppException(ErrorCode.MARKET_DATA_UNAVAILABLE);
        }

        BigDecimal currentPrice = response.getPrice();
        boolean isLong = position.getSide().equals("LONG");
        boolean valid = switch (type) {
            case TAKE_PROFIT -> isLong
                    ? triggerPrice.compareTo(currentPrice) > 0
                    : triggerPrice.compareTo(currentPrice) < 0;
            case STOP_LOSS -> isLong
                    ? triggerPrice.compareTo(currentPrice) < 0
                    : triggerPrice.compareTo(currentPrice) > 0;
        };

        if (!valid) {
            throw new AppException(ErrorCode.INVALID_TRIGGER_PRICE);
        }
    }

    @Transactional
    public void cancelOrderCondition(UUID id, UUID userId) {
        OrderCondition order = orderConditionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_CONDITION_NOT_FOUND));

        if (!order.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.ORDER_NOT_BELONG_TO_USER);
        }
        if (order.getStatus() != OrderConditionStatus.PENDING) {
            throw new AppException(ErrorCode.ORDER_CONDITION_NOT_PENDING);
        }

        order.setStatus(OrderConditionStatus.CANCELLED);
        orderConditionRepository.save(order);

        orderMatchingEngine.unregister(order);
        symbolDemandProducer.releaseSymbol(order.getSymbol());
    }
}
