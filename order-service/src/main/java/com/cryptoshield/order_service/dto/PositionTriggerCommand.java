package com.cryptoshield.order_service.dto;

import com.cryptoshield.order_service.enums.PositionSide;
import com.cryptoshield.order_service.enums.TriggerAction;
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
public class PositionTriggerCommand {
    private TriggerAction action;
    private UUID positionId;
    private String symbol;
    private BigDecimal liquidationPrice;
    private PositionSide side;
}