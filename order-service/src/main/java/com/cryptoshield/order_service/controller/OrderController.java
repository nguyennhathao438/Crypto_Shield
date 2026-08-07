package com.cryptoshield.order_service.controller;

import com.cryptoshield.order_service.dto.request.CloseOrderRequest;
import com.cryptoshield.order_service.dto.response.ApiResponse;
import com.cryptoshield.order_service.dto.request.OrderRequest;
import com.cryptoshield.order_service.dto.response.CloseOrderResponse;
import com.cryptoshield.order_service.dto.response.OrderResponse;
import com.cryptoshield.order_service.service.CloseOrderService;
import com.cryptoshield.order_service.service.OpenOrderService;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderController {
    OpenOrderService openOrderService;
    CloseOrderService closeOrderService;
    @PostMapping("/open/{userId}")
    ResponseEntity<ApiResponse<OrderResponse>> takeOrder(
            @PathVariable("userId") UUID userId,
            @RequestBody @Valid OrderRequest orderRequest){
        return ResponseEntity.ok().body(ApiResponse.<OrderResponse>builder()
                .result(openOrderService.takeOrder(userId,orderRequest))
                .build());
    }
    @PostMapping("/close/{userId}")
    ResponseEntity<ApiResponse<CloseOrderResponse>> closeOrder(
            @PathVariable("userId") UUID userId,
            @RequestBody @Valid CloseOrderRequest closeOrderRequest){
        return ResponseEntity.ok().body(ApiResponse.<CloseOrderResponse>builder()
                .result(closeOrderService.closeOrder(userId,closeOrderRequest))
                .build());
    }
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> cancelLimitOrder(
            @PathVariable("orderId") UUID orderId,
            @RequestHeader("X-User-Id") UUID userId) {

        openOrderService.cancelLimitOrder(orderId, userId);
        return ResponseEntity.noContent().build();
    }
}
