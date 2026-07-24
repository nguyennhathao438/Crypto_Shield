package com.crypto_shield.market_data_service.component;

import com.crypto_shield.market_data_service.dto.PriceResponse;
import io.netty.resolver.DefaultAddressResolverGroup;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BinanceWebSocketClient implements BinanceCommandSender{
    @Value("${binance.ws-base-url}")
    String wsUrl;
    final PriceCache priceCache;
    final SymbolSubscriptionManager subscriptionManager;
    final PricePublisher pricePublisher;
    final WebSocketClient wsClient = new ReactorNettyWebSocketClient(
            HttpClient.create().resolver(DefaultAddressResolverGroup.INSTANCE));
    final ObjectMapper mapper = new ObjectMapper();
    final AtomicInteger requestIdCounter = new AtomicInteger(1);
    final Sinks.Many<String> outboundSink = Sinks.many().unicast().onBackpressureBuffer();

    final ConcurrentHashMap<String, Sinks.Many<PriceResponse>> priceSinks = new ConcurrentHashMap<>();

    @PostConstruct
    public void connect() {
        subscriptionManager.setCommandSender(this);

        wsClient.execute(URI.create(wsUrl), session -> {
                    log.info("Đã kết nối WS Binance Futures");
                    subscriptionManager.resubscribeAll();

                    Mono<Void> receive = session.receive()
                            .map(WebSocketMessage::getPayloadAsText)
                            .doOnNext(this::handleMessage)
                            .doOnError(e -> log.error("Lỗi nhận message: {}", e.getMessage()))
                            .then();

                    Mono<Void> send = session.send(outboundSink.asFlux().map(session::textMessage));

                    return Mono.zip(receive, send).then();
                })
                .doOnError(e -> log.error("Kết nối WS thất bại: {}", e.getMessage()))
                .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(2))
                        .maxBackoff(Duration.ofSeconds(30))
                        .doBeforeRetry(sig -> log.warn("Đang reconnect WS Binance Futures...")))
                .subscribe();
    }
    private void handleMessage(String payload) {
        try {
            JsonNode node = mapper.readTree(payload);

            if (node.has("result")) {
                log.info("ACK subscribe: {}", payload);
                return;
            }

            if (node.has("e") && "24hrTicker".equals(node.get("e").asText())) {
                String symbol = node.get("s").asText();
                double price = node.get("c").asDouble();
                long ts = node.get("E").asLong();
                // 1. Ghi vào cache nội bộ (phục vụ REST/Facade đọc)
                priceCache.update(symbol, price, ts);

                PriceResponse priceResponse = new PriceResponse(symbol, price, ts);

                // 2. Đẩy vào Sinks - phục vụ FE đang stream trực tiếp
                Sinks.Many<PriceResponse> sink = priceSinks.get(symbol);
                if (sink != null) {
                    sink.tryEmitNext(priceResponse);
                }

                // 3. Publish lên Kafka - phục vụ Wallet/Order Service/Liquidation Engine
                pricePublisher.publish(priceResponse);
            }
        } catch (Exception e) {
            log.error("Parse message lỗi: {}", e.getMessage());
        }
    }
    @Override
    public void sendSubscribeCommand(String stream, String method) {
        String request = String.format(
                "{\"method\":\"%s\",\"params\":[\"%s\"],\"id\":%d}",
                method, stream, requestIdCounter.getAndIncrement());
        outboundSink.tryEmitNext(request);
    }
    public reactor.core.publisher.Flux<PriceResponse> getOrCreateSinkFlux(String symbol) {
        String upper = symbol.toUpperCase();
        Sinks.Many<PriceResponse> sink = priceSinks.computeIfAbsent(upper,
                s -> Sinks.many().multicast().onBackpressureBuffer());
        return sink.asFlux();
    }
    public void removeSinkIfUnused(String symbol) {
        priceSinks.remove(symbol.toUpperCase());
    }

}
