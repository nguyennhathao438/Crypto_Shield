package com.crypto_shield.wallet_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionTriggerFiredEvent {
    private UUID positionId;
    private String symbol;
    private BigDecimal triggeredPrice;
    private Instant triggeredAt;

}
