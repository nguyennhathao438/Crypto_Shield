package com.crypto_shield.wallet_service.dto;


import com.crypto_shield.wallet_service.enums.PositionSide;
import com.crypto_shield.wallet_service.enums.TriggerAction;
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