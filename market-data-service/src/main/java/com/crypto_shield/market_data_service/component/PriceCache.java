package com.crypto_shield.market_data_service.component;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PriceCache {
    final ConcurrentHashMap<String, Double> latestPrices = new ConcurrentHashMap<>();
    final ConcurrentHashMap<String, Long> latestTimestamps = new ConcurrentHashMap<>();

    public void update(String symbol, double price, long timestamp) {
        latestPrices.put(symbol, price);
        latestTimestamps.put(symbol, timestamp);
    }
    public Double getPrice(String symbol) {
        return latestPrices.get(symbol);
    }

    public Long getTimestamp(String symbol) {
        return latestTimestamps.get(symbol);
    }

    public boolean hasData(String symbol) {
        return latestPrices.containsKey(symbol);
    }

    public void remove(String symbol) {
        latestPrices.remove(symbol);
        latestTimestamps.remove(symbol);
    }
}
