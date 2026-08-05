package com.cryptoshield.order_service.dto.request;

import com.cryptoshield.order_service.enums.OrderConditionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateOrderConditionRequest {
    @NotNull
    UUID positionId;
    @NotNull
    UUID userId;
    @NotNull
    OrderConditionType type;
    @NotNull @Positive
    BigDecimal triggerPrice;
    @NotNull @Positive
    BigDecimal quantity;
}

