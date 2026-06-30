package com.cryptoshield.order_service.dto;

import com.cryptoshield.order_service.enums.OrderSide;
import com.cryptoshield.order_service.enums.OrderStatus;
import com.cryptoshield.order_service.enums.OrderType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderResponse {
    String symbol;
    OrderType type;

    OrderSide side;

    BigDecimal quantity;

    BigDecimal price;

    BigDecimal margin;

    Integer leverage;

     OrderStatus status;


}
