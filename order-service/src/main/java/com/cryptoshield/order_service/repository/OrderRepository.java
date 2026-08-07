package com.cryptoshield.order_service.repository;


import com.cryptoshield.order_service.entity.Order;
import com.cryptoshield.order_service.enums.OrderStatus;
import com.cryptoshield.order_service.enums.OrderType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") UUID id);
    List<Order> findByStatusAndType(OrderStatus status, OrderType type);
}
