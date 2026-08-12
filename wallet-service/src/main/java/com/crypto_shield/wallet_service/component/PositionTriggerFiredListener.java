package com.crypto_shield.wallet_service.component;

import com.crypto_shield.wallet_service.dto.PositionTriggerFiredEvent;
import com.crypto_shield.wallet_service.enums.PositionStatus;
import com.crypto_shield.wallet_service.repository.PositionRepository;
import com.crypto_shield.wallet_service.service.ClosePositionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PositionTriggerFiredListener {
    private final PositionRepository positionRepository;
    private final ClosePositionService closePositionService;

    @KafkaListener(topics = "position-trigger-fired", groupId = "wallet-service")
    public void onTriggerFired(PositionTriggerFiredEvent event) {
        log.info("Đã nhận position {}",event.getPositionId());
        var position = positionRepository.findById(event.getPositionId()).orElse(null);

        if (position == null) {
            log.warn("Position {} không tồn tại, bỏ qua event fired", event.getPositionId());
            return;
        }

        if (position.getStatus() != PositionStatus.OPEN) {
            log.info("Position {} không còn ACTIVE (status={}), bỏ qua duplicate/late event",
                    event.getPositionId(), position.getStatus());
            return;
        }

        closePositionService.handleLiquidation(position.getId(), event.getTriggeredPrice());
        log.info("Position {} đã liquidation thành công",event.getPositionId());
    }
}
