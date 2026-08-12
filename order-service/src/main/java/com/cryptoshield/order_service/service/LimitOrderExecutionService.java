package com.cryptoshield.order_service.service;

import com.cryptoshield.order_service.components.SymbolDemandProducer;
import com.cryptoshield.order_service.components.WalletServiceGateway;
import com.cryptoshield.order_service.dto.request.OpenPositionRequest;
import com.cryptoshield.order_service.dto.response.OpenPositionResponse;
import com.cryptoshield.order_service.entity.Order;
import com.cryptoshield.order_service.enums.OrderStatus;
import com.cryptoshield.order_service.exception.AppException;
import com.cryptoshield.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class LimitOrderExecutionService {
    private final WalletServiceGateway walletServiceGateway;
    private final OrderRepository orderRepository;
    private final SymbolDemandProducer symbolDemandProducer;

    @Transactional
    public boolean execute(Order orderFromCache, BigDecimal executedPrice) {

        Order order = orderRepository.findByIdForUpdate(orderFromCache.getId()).orElse(null);

        if (order == null || order.getStatus() != OrderStatus.PENDING) {
            log.info("Bỏ qua khớp lệnh LIMIT {} vì không còn ở trạng thái PENDING", orderFromCache.getId());
            return true;
        }

        try {
            BigDecimal notional = executedPrice.multiply(order.getQuantity());
            BigDecimal margin = notional.divide(
                    BigDecimal.valueOf(order.getLeverage()), 8, RoundingMode.HALF_UP);

            OpenPositionRequest request = OpenPositionRequest.builder()
                    .userId(order.getUserId())
                    .symbol(order.getSymbol())
                    .side(order.getSide())
                    .quantity(order.getQuantity())
                    .type(order.getType())
                    .margin(margin)
                    .price(executedPrice)
                    .leverage(order.getLeverage())
                    .build();

            OpenPositionResponse res = walletServiceGateway.openPosition(request);

            order.setStatus(OrderStatus.OPEN);
            order.setEntryPrice(executedPrice);
            order.setMargin(margin);
            orderRepository.save(order);

            log.info("Khớp lệnh LIMIT {} - symbol={}, side={}, giá khớp={}, khối lượng={}, positionId={}",
                    order.getId(), order.getSymbol(), order.getSide(), executedPrice, order.getQuantity(),
                    res.getPosition() != null ? res.getPosition().getPositionId() : null);


        } catch (AppException e) {
            order.setStatus(OrderStatus.FAILED);
            orderRepository.save(order);
            log.error("Khớp lệnh LIMIT {} thất bại vĩnh viễn: {} - {}",
                    order.getId(), e.getErrorCode(), e.getMessage());

        } catch (Exception e) {
            log.error("Lỗi tạm thời khi khớp lệnh LIMIT {}, giữ PENDING để thử lại ở tick giá sau: {}",
                    order.getId(), e.getMessage());
            return false;
        }

        symbolDemandProducer.releaseSymbol(order.getSymbol());
        return true;
    }
}
