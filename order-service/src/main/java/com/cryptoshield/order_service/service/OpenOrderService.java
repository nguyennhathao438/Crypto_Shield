package com.cryptoshield.order_service.service;

import com.cryptoshield.order_service.components.LimitOrderMatchingEngine;
import com.cryptoshield.order_service.components.SymbolDemandProducer;
import com.cryptoshield.order_service.dto.request.OpenPositionRequest;
import com.cryptoshield.order_service.dto.request.OrderRequest;
import com.cryptoshield.order_service.dto.response.OpenPositionResponse;
import com.cryptoshield.order_service.dto.response.OrderResponse;
import com.cryptoshield.order_service.dto.response.PriceResponse;
import com.cryptoshield.order_service.entity.Order;
import com.cryptoshield.order_service.enums.ErrorCode;
import com.cryptoshield.order_service.enums.OrderSide;
import com.cryptoshield.order_service.enums.OrderStatus;
import com.cryptoshield.order_service.enums.OrderType;
import com.cryptoshield.order_service.exception.AppException;
import com.cryptoshield.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenOrderService {
    @Autowired
    OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;

    private static final BigDecimal MAX_SLIPPAGE_PERCENT = BigDecimal.valueOf(0.005);
    private static final Duration CALL_TIMEOUT = Duration.ofSeconds(30);
    private final LimitOrderMatchingEngine limitOrderMatchingEngine;
    private final SymbolDemandProducer symbolDemandProducer;
    @Transactional
    public OrderResponse takeOrder(UUID userId, OrderRequest request) {


        // Get price from data service
        PriceResponse priceData = getMarketPrice(request.getSymbol());
        BigDecimal actualPrice = resolveActualPrice(request, priceData);
        log.info("actualPrice {} - order price {} - type {}",actualPrice, request.getPrice(),request.getType());
        // Check slippage
        if (request.getType() == OrderType.MARKET && request.getPrice() != null) {
            checkSlippage(request.getPrice(), actualPrice);
        }else if(request.getType() == OrderType.LIMIT && request.getPrice() != null){
            if (request.getSide() == OrderSide.BUY &&
                    request.getPrice().compareTo(priceData.getPrice()) >= 0) {
                throw new AppException(ErrorCode.INVALID_LIMIT_PRICE);
            }
            if (request.getSide() == OrderSide.SELL &&
                    request.getPrice().compareTo(priceData.getPrice()) <= 0) {
                throw new AppException(ErrorCode.INVALID_LIMIT_PRICE);
            }
        }

        // Calculated margin
        BigDecimal notional = actualPrice.multiply(request.getQuantity());
        BigDecimal calculatedMargin = notional.divide(
                BigDecimal.valueOf(request.getLeverage()), 8, RoundingMode.HALF_UP);

        OrderStatus status = (request.getType() == OrderType.MARKET)
                ? OrderStatus.OPEN
                : OrderStatus.PENDING;

        // Create order
        Order order = Order.builder()
                .userId(userId)
                .side(request.getSide())
                .type(request.getType())
                .entryPrice(actualPrice)
                .symbol(request.getSymbol())
                .leverage(request.getLeverage())
                .quantity(request.getQuantity())
                .margin(calculatedMargin)
                .status(status)
                .build();
        order = orderRepository.save(order);

        // Create position if type =market
        if (request.getType() == OrderType.MARKET) {
            OpenPositionResponse walletResponse = openPositionOnWallet(order, userId, actualPrice, calculatedMargin);

            if (!walletResponse.isSuccess()) {
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
                throw new AppException(ErrorCode.WALLET_SERVICE_ERROR);
            }
        }else {
            symbolDemandProducer.requestSymbol(order.getSymbol());
            limitOrderMatchingEngine.register(order);

            log.info("Đăng ký lệnh LIMIT {} - symbol={}, side={}, giá đặt={}, khối lượng={}",
                    order.getId(), order.getSymbol(), order.getSide(), order.getEntryPrice(), order.getQuantity());
        }

        return OrderResponse.builder()
                .id(order.getId())
                .side(order.getSide())
                .type(order.getType())
                .price(order.getEntryPrice())
                .symbol(order.getSymbol())
                .leverage(order.getLeverage())
                .quantity(order.getQuantity())
                .margin(order.getMargin())
                .status(order.getStatus())
                .build();
    }
    private PriceResponse getMarketPrice(String symbol) {
        PriceResponse response;
        try {
            WebClient webClient = webClientBuilder.build();

            response = webClient.get()
                            .uri("http://market-data-service/api/price/{symbol}", symbol)
                            .retrieve()
                            .bodyToMono(PriceResponse.class)
                    .block(CALL_TIMEOUT);
        } catch (WebClientException e) {
            log.warn("Error call api :",e);
            throw new AppException(ErrorCode.MARKET_DATA_UNAVAILABLE);

        }

        if (response == null) {
            log.warn("Request null");
            throw new AppException(ErrorCode.MARKET_DATA_UNAVAILABLE);
        }
        return response;
    }
    private OpenPositionResponse openPositionOnWallet(
            Order order, UUID userId, BigDecimal actualPrice, BigDecimal margin) {

        OpenPositionRequest req = OpenPositionRequest.builder()
                .userId(userId)
                .symbol(order.getSymbol())
                .side(order.getSide().name())
                .quantity(order.getQuantity())
                .type(order.getType().toString())
                .margin(margin)
                .price(actualPrice)
                .leverage(order.getLeverage())
                .build();

        OpenPositionResponse response;
        try {
            WebClient webClient = webClientBuilder.build();
            response = webClient.post()
                    .uri("http://wallet-service/internal/position/open")
                    .bodyValue(req)
                    .retrieve()
                    .bodyToMono(OpenPositionResponse.class)
                    .block(CALL_TIMEOUT);
        }  catch (WebClientResponseException e) {
            String errorBody = e.getResponseBodyAsString();
            log.warn("Wallet service error [{}]: {}", e.getStatusCode(), errorBody);

            String walletMessage;
            try {
                OpenPositionResponse errorResponse = new ObjectMapper()
                        .readValue(errorBody, OpenPositionResponse.class);
                walletMessage = errorResponse.getMessage();
            } catch (Exception parseEx) {
                walletMessage = "Wallet service error";
            }

            throw new AppException(ErrorCode.WALLET_SERVICE_ERROR, walletMessage);
        } catch (WebClientException e) {
            log.warn("Error call api :", e);
            throw new AppException(ErrorCode.WALLET_SERVICE_ERROR);
        }

        if (response == null) {
            log.warn("Request null");
            throw new AppException(ErrorCode.WALLET_SERVICE_ERROR);
        }
        return response;
    }
    private BigDecimal resolveActualPrice(OrderRequest request, PriceResponse priceData) {
        if (request.getType() == OrderType.LIMIT) {
            return request.getPrice();
        }
        return switch (request.getSide()) {
            case BUY -> priceData.getPrice();
            case SELL -> priceData.getPrice();
        };
    }
    private void checkSlippage(BigDecimal expectedPrice, BigDecimal actualPrice) {
        BigDecimal slippagePercent = actualPrice.subtract(expectedPrice).abs()
                .divide(expectedPrice, 8, RoundingMode.HALF_UP);
        log.warn("Slipage "+expectedPrice +"-"+ actualPrice+"-"+slippagePercent);
        if (slippagePercent.compareTo(MAX_SLIPPAGE_PERCENT) > 0) {
            throw new AppException(ErrorCode.SLIPPAGE_EXCEEDED);
        }
    }
    @Transactional
    public void cancelLimitOrder(UUID orderId, UUID userId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.ORDER_NOT_BELONG_TO_USER);
        }
        if (order.getType() != OrderType.LIMIT) {
            throw new AppException(ErrorCode.INVALID_ORDER_TYPE);
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new AppException(ErrorCode.ORDER_NOT_PENDING);
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        limitOrderMatchingEngine.unregister(order);
        symbolDemandProducer.releaseSymbol(order.getSymbol());

        log.info("Đã hủy lệnh LIMIT {} theo yêu cầu người dùng", order.getId());
    }
}
