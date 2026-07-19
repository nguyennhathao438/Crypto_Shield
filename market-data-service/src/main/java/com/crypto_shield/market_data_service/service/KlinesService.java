package com.crypto_shield.market_data_service.service;

import com.crypto_shield.market_data_service.dto.Candle;
import com.crypto_shield.market_data_service.dto.KlineRequest;
import io.netty.resolver.DefaultAddressResolverGroup;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.netty.http.client.HttpClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class KlinesService {
    @Value("${binance.rest-base-url}")
    private String baseUrl;

    @Value("${binance.kline-path}")
    private String klinesPath;

    private WebClient webClient;

    @PostConstruct
    private void init() {
        HttpClient httpClient = HttpClient.create()
                .resolver(DefaultAddressResolverGroup.INSTANCE);

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    public List<Candle> getKlines(KlineRequest request) {
        String url = buildUrl(request);

        try {
            List<List<Object>> rawData = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();

            return mapToCandles(rawData);

        } catch (WebClientResponseException e) {
            log.error("Loi khi goi Binance API cho {}/{}: status={} body={}",
                    request.getSymbol(), request.getInterval(),
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException(
                    "Khong lay duoc du lieu nen tu Binance: " + e.getResponseBodyAsString());
        }
    }

    private String buildUrl(KlineRequest request) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(klinesPath)
                .queryParam("symbol", request.getSymbol().toUpperCase())
                .queryParam("interval", request.getInterval())
                .queryParam("limit", request.getLimit() != null ? request.getLimit() : 500);

        if (request.getStartTime() != null) {
            builder.queryParam("startTime", request.getStartTime());
        }
        if (request.getEndTime() != null) {
            builder.queryParam("endTime", request.getEndTime());
        }

        return builder.toUriString();
    }

    @SuppressWarnings("unchecked")
    private List<Candle> mapToCandles(List<List<Object>> rawData) {
        List<Candle> candles = new ArrayList<>();
        if (rawData == null) return candles;

        for (List<Object> c : rawData) {
            candles.add(new Candle(
                    ((Number) c.get(0)).longValue(),
                    String.valueOf(c.get(1)),
                    String.valueOf(c.get(2)),
                    String.valueOf(c.get(3)),
                    String.valueOf(c.get(4)),
                    String.valueOf(c.get(5)),
                    ((Number) c.get(6)).longValue(),
                    String.valueOf(c.get(7)),
                    ((Number) c.get(8)).intValue(),
                    String.valueOf(c.get(9)),
                    String.valueOf(c.get(10))
            ));
        }
        return candles;
    }
}