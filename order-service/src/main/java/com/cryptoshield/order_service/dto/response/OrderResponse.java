package com.cryptoshield.order_service.dto.response;

import com.cryptoshield.order_service.enums.OrderSide;
import com.cryptoshield.order_service.enums.OrderStatus;
import com.cryptoshield.order_service.enums.OrderType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderResponse {
    UUID id;
    String symbol;
    OrderType type;

    OrderSide side;

    BigDecimal quantity;

    BigDecimal price;

    BigDecimal margin;

    Integer leverage;

     OrderStatus status;

}
