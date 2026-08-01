package com.crypto_shield.wallet_service.config;

import com.crypto_shield.wallet_service.dto.PriceEvent;
import com.crypto_shield.wallet_service.service.PositionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PriceEventConsumer {

    private final PriceCache priceCache;
    private final PositionService positionService;
    @KafkaListener(topics = "market-price-updates", groupId = "wallet-service")
    public void onPriceUpdate(PriceEvent event) {
        priceCache.update(event.getSymbol(), event.getPrice(), event.getTimestamp());
        log.debug("Cập nhật giá {}: {}", event.getSymbol(), event.getPrice());
        positionService.notifyPriceChanged(event.getSymbol());
    }
}