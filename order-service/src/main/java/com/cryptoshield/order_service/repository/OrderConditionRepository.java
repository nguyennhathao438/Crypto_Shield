package com.cryptoshield.order_service.repository;

import com.cryptoshield.order_service.entity.Order;
import com.cryptoshield.order_service.entity.OrderCondition;
import com.cryptoshield.order_service.enums.OrderConditionStatus;
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
public interface OrderConditionRepository extends JpaRepository<OrderCondition, UUID> {
    List<OrderCondition> findByStatus(OrderConditionStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(" SELECT oc FROM OrderCondition oc WHERE oc.id = :id")
    Optional<OrderCondition> findByIdForUpdate(@Param("id") UUID id);
    List<OrderCondition> findByPositionIdAndStatusAndIdNot(
            UUID positionId, OrderConditionStatus status, UUID excludeId);
}
