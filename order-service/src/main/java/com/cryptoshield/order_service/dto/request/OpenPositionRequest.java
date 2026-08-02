package com.cryptoshield.order_service.dto.request;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class OpenPositionRequest {
    private UUID userId;
    private String symbol;
    private String type;
    private String side;
    private BigDecimal quantity;
    private BigDecimal margin;
    private BigDecimal price;
    private Integer leverage;
}