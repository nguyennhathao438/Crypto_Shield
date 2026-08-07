package com.cryptoshield.order_service.components;

import com.cryptoshield.order_service.entity.OrderCondition;
import com.cryptoshield.order_service.enums.OrderConditionStatus;
import com.cryptoshield.order_service.enums.OrderSide;
import com.cryptoshield.order_service.repository.OrderConditionRepository;
import com.cryptoshield.order_service.service.OrderExecutionService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class OrderMatchingEngine {
    private final Map<String, List<OrderCondition>> ordersBySymbol = new ConcurrentHashMap<>();
    private final OrderConditionRepository orderConditionRepository;
    private final SymbolDemandProducer symbolDemandProducer;
    private final OrderExecutionService orderExecutionService;
    private final Executor orderExecutionExecutor;
    @PostConstruct
    public void restoreState() {
        List<OrderCondition> pending = orderConditionRepository.findByStatus(OrderConditionStatus.PENDING);
        pending.forEach(order -> {
            register(order);
            symbolDemandProducer.requestSymbol(order.getSymbol());
        });
        log.info("Khôi phục {} lệnh TP/SL đang chờ", pending.size());
    }
    public void register(OrderCondition order) {
        ordersBySymbol
                .computeIfAbsent(order.getSymbol(), s -> new CopyOnWriteArrayList<>())
                .add(order);
    }
    public void unregister(OrderCondition order) {
        List<OrderCondition> list = ordersBySymbol.get(order.getSymbol());
        if (list != null) list.remove(order);
    }
    public void checkOrders(String symbol, BigDecimal currentPrice) {
        List<OrderCondition> orders = ordersBySymbol.get(symbol);
        if (orders == null || orders.isEmpty()) return;

        for (OrderCondition order : orders) {
            if (isTriggered(order, currentPrice)) {
                orderExecutionExecutor.execute(() -> {
                    boolean terminal = orderExecutionService.execute(order, currentPrice);
                    if (terminal) unregister(order);
                });
            }
        }
    }
    private boolean isTriggered(OrderCondition order, BigDecimal currentPrice) {
        boolean isLong = order.getPositionSide() == OrderSide.BUY;
        return switch (order.getType()) {
            case TAKE_PROFIT -> isLong
                    ? currentPrice.compareTo(order.getTriggerPrice()) >= 0
                    : currentPrice.compareTo(order.getTriggerPrice()) <= 0;
            case STOP_LOSS -> isLong
                    ? currentPrice.compareTo(order.getTriggerPrice()) <= 0
                    : currentPrice.compareTo(order.getTriggerPrice()) >= 0;
        };
    }

}
