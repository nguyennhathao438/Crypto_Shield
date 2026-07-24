package com.crypto_shield.market_data_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SymbolDemandEvent {
    private String symbol;
    private String action;      // "NEED" hoặc "RELEASE"
    private String sourceService; // "wallet-service", "order-service" - phục vụ debug/log
}