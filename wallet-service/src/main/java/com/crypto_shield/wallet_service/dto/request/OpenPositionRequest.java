package com.crypto_shield.wallet_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class OpenPositionRequest {
    @NotNull(message = "User ID cannot be null")
    private UUID userId;

    @NotBlank(message = "Symbol cannot be blank")
    private String symbol;

    @NotBlank(message = "Type cannot be blank")
    @Pattern(regexp = "^(MARKET|LIMIT)$", message = "Type must be MARKET or LIMIT")
    private String type;

    @NotBlank(message = "Side cannot be blank")
    @Pattern(regexp = "^(BUY|SELL|LONG|SHORT)$", message = "Side must be BUY, SELL, LONG, or SHORT")
    private String side;

    @NotNull(message = "Quantity cannot be null")
    @Positive(message = "Quantity must be strictly greater than zero")
    private BigDecimal quantity;

    @NotNull(message = "Margin cannot be null")
    @Positive(message = "Margin must be strictly greater than zero")
    private BigDecimal margin;

    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than zero if provided")
    private BigDecimal price;

    @NotNull(message = "Leverage cannot be null")
    @Min(value = 1, message = "Leverage must be at least 1")
    private Integer leverage;
}