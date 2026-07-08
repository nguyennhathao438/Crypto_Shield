package com.cryptoshield.order_service.service;

import com.cryptoshield.order_service.dto.ApiResponse;
import com.cryptoshield.order_service.dto.CheckBalanceResponse;
import com.cryptoshield.order_service.dto.OrderRequest;
import com.cryptoshield.order_service.dto.OrderResponse;
import com.cryptoshield.order_service.entity.Order;
import com.cryptoshield.order_service.enums.ErrorCode;
import com.cryptoshield.order_service.enums.OrderStatus;
import com.cryptoshield.order_service.enums.OrderType;
import com.cryptoshield.order_service.exception.AppException;
import com.cryptoshield.order_service.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

@Service
public class OrderService {
    @Autowired
    OrderRepository orderRepository;
    @Autowired
    RestTemplate restTemplate;

    public OrderResponse takeOrder(UUID userId, OrderRequest request){
        String url = UriComponentsBuilder
                .fromUriString("http://wallet-service/internal/wallets/balance/{userId}")
                .queryParam("margin", request.getMargin())
                .buildAndExpand(userId)
                .toUriString();

        ResponseEntity<ApiResponse<CheckBalanceResponse>> responseEntity =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ApiResponse<CheckBalanceResponse>>() {}
                );

        ApiResponse<CheckBalanceResponse> response = responseEntity.getBody();

        if(!response.getResult().isSuccess()){
            throw new AppException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        OrderStatus status ;
        if(request.getType() == OrderType.MARKET){
            status = OrderStatus.OPEN;
        }else{
            status = OrderStatus.PENDING;
        }
        Order order = Order.builder()
                .userId(userId)
                .side(request.getSide())
                .type(request.getType())
                .entryPrice(request.getPrice())
                .symbol(request.getSymbol())
                .leverage(request.getLeverage())
                .quantity(request.getQuantity())
                .margin(request.getMargin())
                .status(status)
                .build();
        orderRepository.save(order);
        return OrderResponse.builder()
                .side(order.getSide())
                .type(order.getType())
                .price(order.getEntryPrice())
                .symbol(order.getSymbol())
                .leverage(order.getLeverage())
                .quantity(order.getQuantity())
                .margin(order.getMargin())
                .status(status)
                .build();
    }
}
