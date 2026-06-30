package com.cryptoshield.order_service.dto;

import com.cryptoshield.order_service.enums.OrderSide;
import com.cryptoshield.order_service.enums.OrderType;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderRequest {
    @NotBlank(message = "Symbol is required")
    String symbol;
    @NotNull(message = "Order type is required")
    OrderType type;

    @NotNull(message = "Side is required")
     OrderSide side;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.001",
            message = "Quantity must be greater than 0")
    BigDecimal quantity;

    @DecimalMin(value = "0.0",
            inclusive = false,
            message = "Price must be greater than 0")
    BigDecimal price;
    @DecimalMin(value = "0.0",
            inclusive = false,
            message = "Price must be greater than 0")
    BigDecimal margin;

    @NotNull(message = "Leverage is required")
    @Min(value = 1, message = "Leverage must be at least 1")
    @Max(value = 100, message = "Leverage cannot exceed 100")
    Integer leverage;
}
