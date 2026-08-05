package com.cryptoshield.order_service.dto.response;

import com.cryptoshield.order_service.enums.PositionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    private PositionStatus status;
}

