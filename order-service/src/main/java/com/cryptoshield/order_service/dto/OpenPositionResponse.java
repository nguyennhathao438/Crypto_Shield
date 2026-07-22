package com.cryptoshield.order_service.dto;


import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenPositionResponse {
    private boolean success;
    private String message;
    private String errorCode;
    private PositionData position;
    private WalletData wallet;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PositionData {
        private UUID positionId;
        private String symbol;
        private String side;
        private BigDecimal quantity;
        private BigDecimal averageEntryPrice;
        private Integer leverage;
        private BigDecimal margin;
        private BigDecimal liquidationPrice;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WalletData {
        private BigDecimal balance;
        private BigDecimal lockBalance;
    }
}