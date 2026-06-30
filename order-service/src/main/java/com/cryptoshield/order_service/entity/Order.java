package com.cryptoshield.order_service.entity;

import com.cryptoshield.order_service.enums.OrderSide;
import com.cryptoshield.order_service.enums.OrderStatus;
import com.cryptoshield.order_service.enums.OrderType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    @Column(nullable = false, unique = true)
    UUID userId;

    String symbol;
    @Enumerated(EnumType.STRING)
    OrderType type; //BTCUSDT , ETHUSDT
    @Enumerated(EnumType.STRING)
    OrderSide side; //LONG , SHORT
    @Enumerated(EnumType.STRING)
    OrderStatus status; //PENDING , OPEN

    BigDecimal quantity; //0.3

    int leverage;// Don bay
    BigDecimal margin;// So tien bo ra
    BigDecimal entryPrice;// Gia vao lenh




}