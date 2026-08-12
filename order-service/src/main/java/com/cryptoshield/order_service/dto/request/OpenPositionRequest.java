package com.cryptoshield.order_service.dto.request;

import com.cryptoshield.order_service.enums.OrderSide;
import com.cryptoshield.order_service.enums.OrderType;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class OpenPositionRequest {
    @NotNull(message = "userId không được để trống")
    private UUID userId;

    @NotBlank(message = "symbol không được để trống")
    @Pattern(regexp = "^[A-Z0-9]{3,20}$", message = "symbol không đúng định dạng (VD: BTCUSDT)")
    private String symbol;

    @NotNull(message = "type không được để trống")
    private OrderType type;

    @NotNull(message = "side không được để trống")
    private OrderSide side;

    @NotNull(message = "quantity không được để trống")
    @DecimalMin(value = "0.00000001", inclusive = true, message = "quantity phải lớn hơn 0")
    @Digits(integer = 18, fraction = 8, message = "quantity có định dạng số không hợp lệ")
    private BigDecimal quantity;

    @DecimalMin(value = "0.00000001", inclusive = true, message = "margin phải lớn hơn 0")
    @Digits(integer = 18, fraction = 8, message = "margin có định dạng số không hợp lệ")
    private BigDecimal margin;

    @DecimalMin(value = "0.00000001", inclusive = true, message = "price phải lớn hơn 0")
    @Digits(integer = 18, fraction = 8, message = "price có định dạng số không hợp lệ")
    private BigDecimal price;

    @NotNull(message = "leverage không được để trống")
    @Min(value = 1, message = "leverage tối thiểu là 1")
    @Max(value = 125, message = "leverage tối đa là 125")
    private Integer leverage;
}