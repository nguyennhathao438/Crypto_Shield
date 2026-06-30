package com.cryptoshield.order_service.entity;

import com.cryptoshield.order_service.enums.OrderConditionStatus;
import com.cryptoshield.order_service.enums.OrderConditionType;
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
    @Column(nullable = false, unique = true)
    UUID positionId;

    @Enumerated(EnumType.STRING)
    OrderConditionType type;
    BigDecimal quantity;
    @Enumerated(EnumType.STRING)
    OrderConditionStatus status;
}
