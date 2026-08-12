package com.cryptoshield.order_service.components;

import com.cryptoshield.order_service.dto.response.PriceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.swing.text.Position;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarketPriceConsumer {
    private final PriceCache priceCacheService;
    private final OrderMatchingEngine orderMatchingEngine;
    private final LimitOrderMatchingEngine limitOrderMatchingEngine;
    private final PositionTriggerEngine positionTriggerEngine;
    @KafkaListener(topics = "market-price-updates", groupId = "order-service")
    public void onPriceUpdate(PriceResponse priceResponse) {
        priceCacheService.updatePrice(priceResponse.getSymbol(), priceResponse.getPrice());
        orderMatchingEngine.checkOrders(priceResponse.getSymbol(), priceResponse.getPrice());
        limitOrderMatchingEngine.checkOrders(priceResponse.getSymbol(), priceResponse.getPrice());
        positionTriggerEngine.checkTriggers(priceResponse.getSymbol(), priceResponse.getPrice());
    }
}