package com.cryptoshield.order_service.components;

import com.cryptoshield.order_service.entity.Order;
import com.cryptoshield.order_service.enums.OrderSide;
import com.cryptoshield.order_service.enums.OrderStatus;
import com.cryptoshield.order_service.enums.OrderType;
import com.cryptoshield.order_service.repository.OrderRepository;
import com.cryptoshield.order_service.service.LimitOrderExecutionService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

@Component
@RequiredArgsConstructor
@Slf4j
public class LimitOrderMatchingEngine {

    private final Map<String, List<Order>> ordersBySymbol = new ConcurrentHashMap<>();
    private final OrderRepository orderRepository;
    private final SymbolDemandProducer symbolDemandProducer;
    private final LimitOrderExecutionService limitOrderExecutionService;

    @Qualifier("orderExecutionExecutor")
    private final Executor orderExecutionExecutor;


    @PostConstruct
    public void restoreState() {
        List<Order> pending = orderRepository.findByStatusAndType(OrderStatus.PENDING, OrderType.LIMIT);
        pending.forEach(order -> {
            register(order);
            symbolDemandProducer.requestSymbol(order.getSymbol());
        });
        log.info("Khôi phục {} lệnh LIMIT đang chờ", pending.size());
    }

    public void register(Order order) {
        ordersBySymbol
                .computeIfAbsent(order.getSymbol(), s -> new CopyOnWriteArrayList<>())
                .add(order);
    }

    public void unregister(Order order) {
        List<Order> list = ordersBySymbol.get(order.getSymbol());
        if (list != null) list.remove(order);
    }

    public void checkOrders(String symbol, BigDecimal currentPrice) {
        List<Order> orders = ordersBySymbol.get(symbol);
        if (orders == null || orders.isEmpty()) return;

        for (Order order : orders) {
            if (isTriggered(order, currentPrice)) {
                boolean terminal = limitOrderExecutionService.execute(order, currentPrice);
                if (terminal) {
                    unregister(order);
                } else {
                    log.warn("Lệnh LIMIT {} chưa xử lý xong do lỗi tạm thời, giữ lại để thử ở tick giá tiếp theo",
                            order.getId());
                }
            }
        }
    }

    /**
     * BUY limit: người mua muốn mua ở mức giá <= giá đặt (chờ giá giảm xuống)
     * SELL limit: người bán muốn bán ở mức giá >= giá đặt (chờ giá tăng lên)
     */
    private boolean isTriggered(Order order, BigDecimal currentPrice) {
        return order.getSide() == OrderSide.BUY
                ? currentPrice.compareTo(order.getEntryPrice()) <= 0
                : currentPrice.compareTo(order.getEntryPrice()) >= 0;
    }
}