package com.cryptoshield.order_service.components;

import com.cryptoshield.order_service.dto.PositionTrigger;
import com.cryptoshield.order_service.dto.PositionTriggerCommand;
import com.cryptoshield.order_service.enums.TriggerAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PositionTriggerCommandListener {

    private final PositionTriggerEngine positionTriggerEngine;

    @KafkaListener(topics = "position-trigger-commands", groupId = "order-service")
    public void onTriggerCommand(PositionTriggerCommand cmd) {
        if (cmd.getAction() == TriggerAction.REGISTER) {
            positionTriggerEngine.register(new PositionTrigger(
                    cmd.getPositionId(), cmd.getSymbol(), cmd.getLiquidationPrice(), cmd.getSide()
            ));
            log.info("Đăng ký trigger cho position {}", cmd.getPositionId());
        } else if (cmd.getAction() == TriggerAction.CANCEL) {
            positionTriggerEngine.cancel(
                    cmd.getPositionId(), cmd.getSymbol(), cmd.getLiquidationPrice(), cmd.getSide()
            );
            log.info("Huỷ trigger cho position {}", cmd.getPositionId());
        }
    }
}