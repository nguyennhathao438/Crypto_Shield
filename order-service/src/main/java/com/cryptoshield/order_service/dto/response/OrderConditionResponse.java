package com.cryptoshield.order_service.dto.response;

import com.cryptoshield.order_service.entity.OrderCondition;
import com.cryptoshield.order_service.enums.OrderConditionStatus;
import com.cryptoshield.order_service.enums.OrderConditionType;
import com.cryptoshield.order_service.enums.OrderSide;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderConditionResponse {
    private UUID id;
    private UUID positionId;
    private String symbol;
    private OrderSide positionSide;
    private OrderConditionType type;
    private BigDecimal triggerPrice;
    private BigDecimal quantity;
    private OrderConditionStatus status;

    public static OrderConditionResponse from(OrderCondition entity) {
        return OrderConditionResponse.builder()
                .id(entity.getId())
                .positionId(entity.getPositionId())
                .symbol(entity.getSymbol())
                .positionSide(entity.getPositionSide())
                .type(entity.getType())
                .triggerPrice(entity.getTriggerPrice())
                .quantity(entity.getQuantity())
                .status(entity.getStatus())
                .build();
    }
}
