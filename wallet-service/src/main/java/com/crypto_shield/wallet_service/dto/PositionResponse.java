package com.crypto_shield.wallet_service.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PositionResponse {
    private UUID positionId;
    private String symbol;
    private String side;
    private BigDecimal quantity;
    private BigDecimal averageEntryPrice;
    private BigDecimal currentPrice;
    private Integer leverage;
    private BigDecimal margin;
    private BigDecimal liquidationPrice;
    private BigDecimal unrealizedPnl;
    private BigDecimal unrealizedPnlPercent;
}
