package com.crypto_shield.wallet_service.entity;

import com.crypto_shield.wallet_service.enums.PositionSide;
import com.crypto_shield.wallet_service.enums.PositionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "side", nullable = false, length = 10)
    private PositionSide side;

    @Column(name = "quantity", nullable = false, precision = 20, scale = 8)
    private BigDecimal quantity;

    @Column(name = "average_entry_price", nullable = false, precision = 20, scale = 8)
    private BigDecimal averageEntryPrice;

    @Column(name = "leverage", nullable = false)
    private Integer leverage;

    @Column(name = "margin", nullable = false, precision = 20, scale = 8)
    private BigDecimal margin;

    @Column(name = "liquidation_price", nullable = false, precision = 20, scale = 8)
    private BigDecimal liquidationPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    @Builder.Default
    private PositionStatus status = PositionStatus.OPEN;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void calculateLiquidationPrice() {
        BigDecimal marginPerUnit = this.margin.divide(this.quantity, 8, RoundingMode.HALF_UP);

        if (this.side == PositionSide.LONG) {
            this.liquidationPrice = this.averageEntryPrice.subtract(marginPerUnit);
        } else {
            this.liquidationPrice = this.averageEntryPrice.add(marginPerUnit);
        }
    }
}
