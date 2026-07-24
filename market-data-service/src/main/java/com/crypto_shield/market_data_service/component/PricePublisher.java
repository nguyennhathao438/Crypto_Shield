package com.crypto_shield.market_data_service.component;

import com.crypto_shield.market_data_service.dto.PriceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PricePublisher {

    private final KafkaTemplate<String, PriceResponse> kafkaTemplate;

    private static final String TOPIC = "market-price-updates";

    public void publish(PriceResponse priceResponse) {
        kafkaTemplate.send(TOPIC, priceResponse.getSymbol(), priceResponse);
    }
}