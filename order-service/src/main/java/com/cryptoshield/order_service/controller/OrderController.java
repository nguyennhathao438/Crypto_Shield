package com.cryptoshield.order_service.controller;

import com.cryptoshield.order_service.dto.ApiResponse;
import com.cryptoshield.order_service.dto.OrderRequest;
import com.cryptoshield.order_service.dto.OrderResponse;
import com.cryptoshield.order_service.service.OrderService;
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
    OrderService orderService;
    @PostMapping("/{userId}")
    ResponseEntity<ApiResponse<OrderResponse>> takeOrder(
            @PathVariable UUID userId,
            @RequestBody @Valid OrderRequest orderRequest){
        return ResponseEntity.ok().body(ApiResponse.<OrderResponse>builder()
                .result(orderService.takeOrder(userId,orderRequest))
                .build());
    }
}
