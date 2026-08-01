package com.crypto_shield.wallet_service.dto;

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
    private String action;
    private String sourceService;
}