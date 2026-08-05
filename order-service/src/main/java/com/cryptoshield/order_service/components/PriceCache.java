package com.cryptoshield.order_service.components;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PriceCache{
    private final Map<String, BigDecimal> currentPrices = new ConcurrentHashMap<>();

    public void updatePrice(String symbol, BigDecimal price) {
        currentPrices.put(symbol, price);
    }

    public Optional<BigDecimal> getPrice(String symbol) {
        return Optional.ofNullable(currentPrices.get(symbol));
    }
}