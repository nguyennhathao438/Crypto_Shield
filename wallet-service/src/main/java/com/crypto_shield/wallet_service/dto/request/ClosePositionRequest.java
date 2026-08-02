package com.crypto_shield.wallet_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class ClosePositionRequest {

    @NotNull(message = "User ID cannot be null")
    private UUID userId;

    @NotNull(message = "Position ID cannot be null")
    private UUID positionId;

    @NotBlank(message = "Symbol cannot be blank")
    private String symbol;

    @NotNull(message = "Current price cannot be null")
    @Positive(message = "Current price must be strictly greater than zero")
    private BigDecimal currentPrice;

    @NotNull(message = "Closed quantity cannot be null")
    @Positive(message = "Closed quantity must be strictly greater than zero")
    private BigDecimal closedQuantity;
}