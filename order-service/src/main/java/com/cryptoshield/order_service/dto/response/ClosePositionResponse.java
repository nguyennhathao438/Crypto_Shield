package com.cryptoshield.order_service.dto.response;

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
public class ClosePositionResponse {
    private boolean success;
    private String message;
    private String errorCode;

    private UUID orderId;
    private BigDecimal executedPrice;
    private BigDecimal closedQuantity;
    private BigDecimal realizedPnl;

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
        private BigDecimal quantity;         // số lượng còn lại sau khi đóng
        private BigDecimal averageEntryPrice;
        private Integer leverage;
        private BigDecimal margin;
        private BigDecimal liquidationPrice;  // null nếu đã CLOSED hẳn
        private String status;                //CLOSE nếu đóng 100%
        private BigDecimal totalRealizedPnl;
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
