package com.crypto_shield.wallet_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceEvent {
    private String symbol;
    private double price;
    private long timestamp;
}
