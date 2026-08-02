package com.cryptoshield.order_service.dto.request;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;
@Getter
@Builder
public class ClosePositionRequest {
    private UUID userId;
    private UUID positionId;
    private String symbol;
    private BigDecimal currentPrice;
    private BigDecimal closedQuantity;
}
