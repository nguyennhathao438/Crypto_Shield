package com.cryptoshield.order_service.dto;

import com.cryptoshield.order_service.enums.PositionSide;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PositionTrigger {
    private UUID positionId;
    private String symbol;
    private BigDecimal triggerPrice;
    private PositionSide side;
}