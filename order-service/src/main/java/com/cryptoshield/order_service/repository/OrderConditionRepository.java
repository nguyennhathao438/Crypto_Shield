package com.cryptoshield.order_service.repository;

import com.cryptoshield.order_service.entity.Order;
import com.cryptoshield.order_service.entity.OrderCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrderConditionRepository extends JpaRepository<OrderCondition, UUID> {

}
