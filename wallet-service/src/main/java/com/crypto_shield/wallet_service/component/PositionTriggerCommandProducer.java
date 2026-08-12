package com.crypto_shield.wallet_service.component;

import com.crypto_shield.wallet_service.dto.PositionTriggerCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PositionTriggerCommandProducer {

    private static final String TOPIC = "position-trigger-commands";

    private final KafkaTemplate<String, PositionTriggerCommand> kafkaTemplate;

    public void send(PositionTriggerCommand command) {
        kafkaTemplate.send(TOPIC, command.getPositionId().toString(), command);
        log.info("Gửi {} trigger cho position {}", command.getAction(), command.getPositionId());
    }
}