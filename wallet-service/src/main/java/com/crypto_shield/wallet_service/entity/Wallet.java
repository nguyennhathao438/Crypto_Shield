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
    private BigDecimal unrealizedPnl;
}