package com.crypto_shield.market_data_service.controller;

import com.crypto_shield.market_data_service.component.BinanceWebSocketClient;
import com.crypto_shield.market_data_service.component.SymbolSubscriptionManager;
import com.crypto_shield.market_data_service.dto.PriceResponse;
import com.crypto_shield.market_data_service.service.PriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/price")
public class PriceController {
    private final PriceService streamPriceService;
    private final BinanceWebSocketClient binanceWebSocketClient;
    private final SymbolSubscriptionManager subscriptionManager;

    @GetMapping("/{type}")
    public Mono<ResponseEntity<PriceResponse>> getCurrentPrice(@PathVariable("type") String type) {
        String symbol = type.toUpperCase();

        // đảm bảo đã subscribe (nếu chưa thì subscribe, nếu rồi thì bỏ qua)
        streamPriceService.subscribe(symbol);

        return Mono.defer(() -> {
                    if (streamPriceService.hasData(symbol)) {
                        return Mono.just(ResponseEntity.ok(new PriceResponse(
                                symbol,
                                streamPriceService.getLatestPrice(symbol),
                                streamPriceService.getLatestTimestamp(symbol)
                        )));
                    }
                    return Mono.error(new RuntimeException("Chưa có dữ liệu"));
                })
                .retryWhen(Retry.fixedDelay(20, Duration.ofMillis(200))) // đợi tối đa ~4s cho lần subscribe đầu
                .onErrorResume(e -> Mono.just(ResponseEntity.status(202)
                        .body(new PriceResponse(symbol, -1, 0))));
    }
    @GetMapping(value = "/stream/{symbol}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<PriceResponse> streamPrice(@PathVariable("symbol") String symbol) {
        String upper = symbol.toUpperCase();

        subscriptionManager.onStreamClientConnect(upper);

        return binanceWebSocketClient.getOrCreateSinkFlux(upper)
                .doFinally(signalType -> subscriptionManager.onStreamClientDisconnect(upper));
    }
}
