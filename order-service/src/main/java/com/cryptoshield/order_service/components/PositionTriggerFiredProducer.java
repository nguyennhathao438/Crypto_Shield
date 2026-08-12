package com.cryptoshield.order_service.components;

import com.cryptoshield.order_service.dto.PositionTrigger;
import com.cryptoshield.order_service.dto.PositionTriggerFiredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class PositionTriggerFiredProducer {
    private static final String TOPIC = "position-trigger-fired";
    private final KafkaTemplate<String, PositionTriggerFiredEvent> kafkaTemplate;
    public boolean publish(PositionTrigger trigger, BigDecimal currentPrice) {
        try {
            PositionTriggerFiredEvent event = PositionTriggerFiredEvent.builder()
                    .positionId(trigger.getPositionId())
                    .symbol(trigger.getSymbol())
                    .triggeredPrice(currentPrice)
                    .triggeredAt(Instant.now())
                    .build();
            kafkaTemplate.send(TOPIC, trigger.getPositionId().toString(), event).get(); // đợi ack
            return true;
        } catch (Exception e) {
            log.error("Publish trigger fired thất bại cho position {}", trigger.getPositionId(), e);
            return false;
        }
    }
}
