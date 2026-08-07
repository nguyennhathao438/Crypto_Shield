package com.cryptoshield.order_service.controller;

import com.cryptoshield.order_service.dto.request.CreateOrderConditionRequest;
import com.cryptoshield.order_service.dto.response.OrderConditionResponse;
import com.cryptoshield.order_service.service.OrderConditionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/order-conditions")
@RequiredArgsConstructor
public class OrderConditionController {
    private final OrderConditionService orderConditionService;

    @PostMapping
    public ResponseEntity<OrderConditionResponse> create(
            @Valid @RequestBody CreateOrderConditionRequest request) {
        return ResponseEntity.ok(orderConditionService.createOrderCondition(request));
    }
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> cancelLimitOrder(
            @PathVariable("ocId") UUID ocId,
            @RequestHeader("X-User-Id") UUID userId) {

        orderConditionService.cancelOrderCondition(ocId,userId);
        return ResponseEntity.noContent().build();
    }
}
