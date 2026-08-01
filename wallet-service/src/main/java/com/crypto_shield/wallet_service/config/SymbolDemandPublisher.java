package com.crypto_shield.wallet_service.config;

import com.crypto_shield.wallet_service.dto.SymbolDemandEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.springframework.boot.availability.AvailabilityChangeEvent.publish;

@Slf4j
@Component
@RequiredArgsConstructor
public class SymbolDemandPublisher {
    private final KafkaTemplate<String, SymbolDemandEvent> kafkaTemplate;

    private static final String TOPIC = "symbol-demand-events";
    private static final String SOURCE_SERVICE = "wallet-service";

    public void publishNeed(String symbol) {
        publish(symbol, "NEED");
    }

    public void publishRelease(String symbol) {
        publish(symbol, "RELEASE");
    }
    private void publish(String symbol, String action) {
        SymbolDemandEvent event = SymbolDemandEvent.builder()
                .symbol(symbol.toUpperCase())
                .action(action)
                .sourceService(SOURCE_SERVICE)
                .build();

        // key = symbol -> đảm bảo NEED/RELEASE cùng 1 symbol luôn xử lý đúng thứ tự
        kafkaTemplate.send(TOPIC, event.getSymbol(), event);
        log.info("Published {} cho symbol {}", action, event.getSymbol());
    }

    public void publishNeedBatch(List<String> symbols) {
        symbols.stream()
                .distinct()
                .forEach(this::publishNeed);
    }
    public void publishReleaseBatch(List<String> symbols) {
        symbols.stream()
                .distinct()
                .forEach(this::publishRelease);
    }
}
