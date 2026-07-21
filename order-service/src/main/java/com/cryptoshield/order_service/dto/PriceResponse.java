package com.cryptoshield.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class PriceResponse {
    private String symbol;
    private BigDecimal price;
    private long timestamp;
}

