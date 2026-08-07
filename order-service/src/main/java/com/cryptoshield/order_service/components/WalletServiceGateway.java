package com.cryptoshield.order_service.components;

import com.cryptoshield.order_service.dto.request.ClosePositionRequest;
import com.cryptoshield.order_service.dto.request.OpenPositionRequest;
import com.cryptoshield.order_service.dto.response.ClosePositionResponse;
import com.cryptoshield.order_service.dto.response.OpenPositionResponse;
import com.cryptoshield.order_service.enums.ErrorCode;
import com.cryptoshield.order_service.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Component
@Slf4j
public class WalletServiceGateway {
    private final WebClient webClient;
    private static final Duration CALL_TIMEOUT = Duration.ofSeconds(30);
    public WalletServiceGateway(@Qualifier("walletServiceWebClient") WebClient webClient) {
        this.webClient = webClient;
    }
    public ClosePositionResponse closePosition(ClosePositionRequest request) {
        return call(
                () -> webClient.post()
                        .uri("/internal/position/close")
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(ClosePositionResponse.class)
                        .block(CALL_TIMEOUT),
                ClosePositionResponse::isSuccess
        );
    }
    public OpenPositionResponse openPosition(OpenPositionRequest request) {
        return call(
                () -> webClient.post()
                        .uri("/internal/position/open")
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(OpenPositionResponse.class)
                        .block(CALL_TIMEOUT),
                OpenPositionResponse::isSuccess
        );
    }
    private <T> T call(Supplier<T> action, Predicate<T> isSuccess) {
        try {
            T response = action.get();

            if (response == null || !isSuccess.test(response)) {
                throw new AppException(ErrorCode.WALLET_SERVICE_ERROR);
            }
            return response;

        } catch (WebClientResponseException.Conflict e) {
            throw new AppException(ErrorCode.POSITION_ALREADY_CLOSED);

        } catch (WebClientResponseException.NotFound e) {
            throw new AppException(ErrorCode.POSITION_NOT_FOUND);

        } catch (WebClientResponseException e) {
            String errorBody = e.getResponseBodyAsString();
            log.warn("Wallet service error [{}]: {}", e.getStatusCode(), errorBody);
            throw new AppException(ErrorCode.WALLET_SERVICE_ERROR, extractMessage(errorBody));

        } catch (AppException e) {
            throw e;

        } catch (Exception e) {
            log.error("Không gọi được wallet-service: {}", e.getMessage());
            throw new AppException(ErrorCode.WALLET_SERVICE_TIMEOUT);
        }
    }
    private String extractMessage(String errorBody) {
        try {
            Map<?, ?> map = new ObjectMapper().readValue(errorBody, Map.class);
            Object msg = map.get("message");
            return msg != null ? msg.toString() : "Wallet service error";
        } catch (Exception parseEx) {
            return "Wallet service error";
        }
    }
}
