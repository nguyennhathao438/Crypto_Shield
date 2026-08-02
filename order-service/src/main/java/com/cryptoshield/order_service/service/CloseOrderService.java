package com.cryptoshield.order_service.service;

import com.cryptoshield.order_service.dto.request.CloseOrderRequest;
import com.cryptoshield.order_service.dto.request.ClosePositionRequest;
import com.cryptoshield.order_service.dto.response.CloseOrderResponse;
import com.cryptoshield.order_service.dto.response.PriceResponse;
import com.cryptoshield.order_service.enums.ErrorCode;
import com.cryptoshield.order_service.exception.AppException;
import com.cryptoshield.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
public class CloseOrderService {
    @Autowired
    OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;

    private static final BigDecimal MAX_SLIPPAGE_PERCENT = BigDecimal.valueOf(0.005);
    private static final Duration CALL_TIMEOUT = Duration.ofSeconds(30);
    public CloseOrderResponse closeOrder(UUID userId, CloseOrderRequest request) {
        // Get price from data service
        PriceResponse priceData = getMarketPrice(request.getSymbol());

        checkSlippage(request.getCurrentPrice(), priceData.getPrice());

        return closePositionOnWallet(request,userId);
    }
    private CloseOrderResponse closePositionOnWallet(CloseOrderRequest request,UUID userId) {

        ClosePositionRequest req = ClosePositionRequest.builder()
                .closedQuantity(request.getClosedQuantity())
                .currentPrice(request.getCurrentPrice())
                .positionId(request.getPositionId())
                .symbol(request.getSymbol())
                .userId(userId)
                .build();
        CloseOrderResponse response;
        try {
            WebClient webClient = webClientBuilder.build();
            response = webClient.post()
                    .uri("http://wallet-service/internal/position/close")
                    .bodyValue(req)
                    .retrieve()
                    .bodyToMono(CloseOrderResponse.class)
                    .block(CALL_TIMEOUT);
        }  catch (WebClientResponseException e) {
            String errorBody = e.getResponseBodyAsString();
            log.warn("Wallet service error [{}]: {}", e.getStatusCode(), errorBody);

            String walletMessage;
            try {
                CloseOrderResponse errorResponse = new ObjectMapper()
                        .readValue(errorBody, CloseOrderResponse.class);
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
    private void checkSlippage(BigDecimal expectedPrice, BigDecimal actualPrice) {
        BigDecimal slippagePercent = actualPrice.subtract(expectedPrice).abs()
                .divide(expectedPrice, 8, RoundingMode.HALF_UP);
        log.warn("Slipage "+expectedPrice +"-"+ actualPrice+"-"+slippagePercent);
        if (slippagePercent.compareTo(MAX_SLIPPAGE_PERCENT) > 0) {
            throw new AppException(ErrorCode.SLIPPAGE_EXCEEDED);
        }
    }
}
