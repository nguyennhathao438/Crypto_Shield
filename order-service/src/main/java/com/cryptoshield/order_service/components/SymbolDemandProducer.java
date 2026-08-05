package com.cryptoshield.order_service.components;

import com.cryptoshield.order_service.dto.SymbolDemandEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class SymbolDemandProducer {
    private final KafkaTemplate<String, SymbolDemandEvent> kafkaTemplate;
    private static final String TOPIC = "symbol-demand-events";
    private final Map<String, AtomicInteger> demandCount = new ConcurrentHashMap<>();

    public void requestSymbol(String symbol) {
        int count = demandCount.computeIfAbsent(symbol, s -> new AtomicInteger(0)).incrementAndGet();
        if (count == 1) {
            publish(symbol, "NEED");
        }
    }

    public void releaseSymbol(String symbol) {
        AtomicInteger counter = demandCount.get(symbol);
        if (counter == null) return;
        if (counter.decrementAndGet() <= 0) {
            demandCount.remove(symbol);
            publish(symbol, "RELEASE");
        }
    }

    private void publish(String symbol, String action) {
        kafkaTemplate.send(TOPIC, symbol, new SymbolDemandEvent(symbol, action));
        log.info("Gửi demand event: symbol={}, action={}", symbol, action);
    }
}