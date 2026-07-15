package com.crypto_shield.market_data_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PriceResponse {
    private String symbol;
    private double price;
    private long timestamp;
}
