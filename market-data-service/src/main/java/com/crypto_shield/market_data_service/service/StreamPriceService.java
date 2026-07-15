package com.crypto_shield.market_data_service.service;

import io.netty.resolver.DefaultAddressResolverGroup;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StreamPriceService {
    @Value("${binance.ws-base-url}")
    String wsUrl;
    final WebSocketClient wsClient = new ReactorNettyWebSocketClient(
            HttpClient.create()
                    .resolver(DefaultAddressResolverGroup.INSTANCE)
    );
    final ConcurrentHashMap<String, Double> latestPrices = new ConcurrentHashMap<>();
    final ConcurrentHashMap<String, Long> latestTimestamps = new ConcurrentHashMap<>();
    final ObjectMapper mapper = new ObjectMapper();

    final Set<String> subscribedSymbols = ConcurrentHashMap.newKeySet();
    final AtomicInteger requestIdCounter = new AtomicInteger(1);

    final AtomicReference<WebSocketSession> currentSession = new AtomicReference<>();
    final Sinks.Many<String> outboundSink = Sinks.many().unicast().onBackpressureBuffer();


    @PostConstruct
    public void connect() {
        wsClient.execute(URI.create(wsUrl), session -> {
                    currentSession.set(session);
                    log.info("Đã kết nối WS Binance Futures");

                    // resubscribe lại tất cả symbol cũ nếu đây là lần reconnect
                    resubscribeAll();

                    Mono<Void> receive = session.receive()
                            .map(WebSocketMessage::getPayloadAsText)
                            .doOnNext(this::handleMessage)
                            .doOnError(e -> log.error("Lỗi nhận message: {}", e.getMessage()))
                            .then();

                    // luồng gửi: mọi message được đẩy vào outboundSink sẽ được gửi qua session này
                    Mono<Void> send = session.send(
                            outboundSink.asFlux().map(session::textMessage)
                    );

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

            // message ACK của lệnh SUBSCRIBE có dạng {"result":null,"id":1} -> bỏ qua
            if (node.has("result")) {
                log.info("ACK subscribe: {}", payload);
                return;
            }

            // message ticker có field "e":"24hrTicker", "s":"BTCUSDT", "c": giá hiện tại
            if (node.has("e") && "24hrTicker".equals(node.get("e").asText())) {
                String symbol = node.get("s").asText();
                double price = node.get("c").asDouble();
                long ts = node.get("E").asLong(); // event time

                latestPrices.put(symbol, price);
                latestTimestamps.put(symbol, ts);
            }
        } catch (Exception e) {
            log.error("Parse message lỗi: {}", e.getMessage());
        }

    }
    public void subscribe(String symbol) {
        String stream = symbol.toLowerCase() + "@ticker";
        if (subscribedSymbols.add(symbol.toUpperCase())) {
            sendSubscribeCommand(stream, "SUBSCRIBE");
            log.info("Đã gửi lệnh subscribe cho {}", symbol);
        }
    }

    public void unsubscribe(String symbol) {
        String stream = symbol.toLowerCase() + "@ticker";
        if (subscribedSymbols.remove(symbol.toUpperCase())) {
            sendSubscribeCommand(stream, "UNSUBSCRIBE");
            latestPrices.remove(symbol.toUpperCase());
            latestTimestamps.remove(symbol.toUpperCase());
            log.info("Đã gửi lệnh unsubscribe cho {}", symbol);
        }
    }

    private void sendSubscribeCommand(String stream, String method) {
        String request = String.format(
                "{\"method\":\"%s\",\"params\":[\"%s\"],\"id\":%d}",
                method, stream, requestIdCounter.getAndIncrement()
        );
        outboundSink.tryEmitNext(request);
    }

    private void resubscribeAll() {
        subscribedSymbols.forEach(symbol -> {
            String stream = symbol.toLowerCase() + "@ticker";
            sendSubscribeCommand(stream, "SUBSCRIBE");
        });
    }

    public Double getLatestPrice(String symbol) {
        return latestPrices.get(symbol.toUpperCase());
    }

    public Long getLatestTimestamp(String symbol) {
        return latestTimestamps.get(symbol.toUpperCase());
    }

    public boolean hasData(String symbol) {
        return latestPrices.containsKey(symbol.toUpperCase());
    }
}
