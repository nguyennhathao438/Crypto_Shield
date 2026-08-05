package com.cryptoshield.order_service.entity;

import com.cryptoshield.order_service.enums.OrderConditionStatus;
import com.cryptoshield.order_service.enums.OrderConditionType;
import com.cryptoshield.order_service.enums.OrderSide;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_conditions")
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
public class OrderCondition {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    @Column(nullable = false)
    UUID positionId;

    @Column(nullable = false)
    UUID userId;

    @Enumerated(EnumType.STRING)
    OrderSide positionSide;

    @Enumerated(EnumType.STRING)
    OrderConditionType type;

    BigDecimal quantity;
    String symbol;
    @Enumerated(EnumType.STRING)
    OrderConditionStatus status;

    @Column(nullable = false)
    BigDecimal triggerPrice;

}
