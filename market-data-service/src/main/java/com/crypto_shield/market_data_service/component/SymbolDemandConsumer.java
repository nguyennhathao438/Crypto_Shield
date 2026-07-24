package com.crypto_shield.market_data_service.component;

import com.crypto_shield.market_data_service.dto.SymbolDemandEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SymbolDemandConsumer {

    private final SymbolSubscriptionManager subscriptionManager;

    @KafkaListener(topics = "symbol-demand-events", groupId = "market-data-service")
    public void onDemandEvent(SymbolDemandEvent event) {
        log.info("Nhận demand event: {}", event);

        switch (event.getAction()) {
            case "NEED" -> subscriptionManager.onBusinessDemandIncrease(event.getSymbol());
            case "RELEASE" -> subscriptionManager.onBusinessDemandDecrease(event.getSymbol());
            default -> log.warn("Action không hợp lệ: {}", event.getAction());
        }
    }
}
