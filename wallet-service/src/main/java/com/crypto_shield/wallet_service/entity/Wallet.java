package com.crypto_shield.wallet_service.entity;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name ="wallets")
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    @Column(nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal balance;

    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal lockBalance;
    public void lockMargin(BigDecimal margin) {
        this.balance = this.balance.subtract(margin);
        this.lockBalance = this.lockBalance.add(margin);
    }
    public void unlockMargin(BigDecimal margin,BigDecimal realizedPnl){
        this.balance = this.balance.add(margin.add(realizedPnl));
        this.lockBalance = this.lockBalance.subtract(margin);
    }
}