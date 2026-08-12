package com.crypto_shield.wallet_service.dto;

import com.crypto_shield.wallet_service.enums.PositionSide;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class PositionTrigger {
    private UUID positionId;
    private String symbol;
    private BigDecimal triggerPrice;
    private PositionSide side;
}