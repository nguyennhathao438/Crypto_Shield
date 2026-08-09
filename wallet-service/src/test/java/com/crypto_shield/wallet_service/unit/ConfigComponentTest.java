package com.crypto_shield.wallet_service.unit;

import com.crypto_shield.wallet_service.config.PriceCache;
import com.crypto_shield.wallet_service.config.PriceEventConsumer;
import com.crypto_shield.wallet_service.config.SymbolDemandPublisher;
import com.crypto_shield.wallet_service.dto.PriceEvent;
import com.crypto_shield.wallet_service.dto.SymbolDemandEvent;
import com.crypto_shield.wallet_service.service.PositionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("Config Component Unit Tests")
class ConfigComponentTest {

    @Test
    @DisplayName("Should update and retrieve latest price and timestamp")
    void priceCache_updateAndRead_returnsLatestValues() {
        // Given
        PriceCache priceCache = new PriceCache();

        // When
        priceCache.update("BTCUSDT", 101.25, 12345L);

        // Then
        assertThat(priceCache.getPrice("BTCUSDT")).isEqualTo(101.25);
        assertThat(priceCache.getTimestamp("BTCUSDT")).isEqualTo(12345L);
        assertThat(priceCache.getPrice("ETHUSDT")).isNull();
        assertThat(priceCache.getTimestamp("ETHUSDT")).isNull();
    }

    @Test
    @DisplayName("Should cache price event and notify position service")
    void onPriceUpdate_validEvent_updatesCacheAndNotifiesPositions() {
        // Given
        PriceCache priceCache = mock(PriceCache.class);
        PositionService positionService = mock(PositionService.class);
        PriceEventConsumer consumer = new PriceEventConsumer(priceCache, positionService);
        PriceEvent event = new PriceEvent("BTCUSDT", 42000.5, 999L);

        // When
        consumer.onPriceUpdate(event);

        // Then
        verify(priceCache).update("BTCUSDT", 42000.5, 999L);
        verify(positionService).notifyPriceChanged("BTCUSDT");
    }

    @Test
    @DisplayName("Should publish distinct need events with uppercase symbols")
    void publishNeedBatch_duplicateSymbols_sendsDistinctUppercaseEvents() {
        // Given
        KafkaTemplate<String, SymbolDemandEvent> kafkaTemplate = mock(KafkaTemplate.class);
        SymbolDemandPublisher publisher = new SymbolDemandPublisher(kafkaTemplate);

        // When
        publisher.publishNeedBatch(List.of("btcusdt", "btcusdt", "ethusdt"));

        // Then
        ArgumentCaptor<SymbolDemandEvent> eventCaptor = ArgumentCaptor.forClass(SymbolDemandEvent.class);
//        verify(kafkaTemplate).send("symbol-demand-events", "BTCUSDT", eventCaptor.capture());
        verify(kafkaTemplate).send(eq("symbol-demand-events"), eq("BTCUSDT"), eventCaptor.capture());
        verify(kafkaTemplate).send(eq("symbol-demand-events"), eq("ETHUSDT"), eventCaptor.capture());
        assertThat(eventCaptor.getAllValues())
                .extracting(SymbolDemandEvent::getAction)
                .containsExactly("NEED", "NEED");
        assertThat(eventCaptor.getAllValues())
                .extracting(SymbolDemandEvent::getSourceService)
                .containsExactly("wallet-service", "wallet-service");
        verifyNoMoreInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("Should publish release event")
    void publishRelease_singleSymbol_sendsReleaseEvent() {
        // Given
        KafkaTemplate<String, SymbolDemandEvent> kafkaTemplate = mock(KafkaTemplate.class);
        SymbolDemandPublisher publisher = new SymbolDemandPublisher(kafkaTemplate);

        // When
        publisher.publishRelease("btcusdt");

        // Then
        ArgumentCaptor<SymbolDemandEvent> eventCaptor = ArgumentCaptor.forClass(SymbolDemandEvent.class);
        verify(kafkaTemplate).send(eq("symbol-demand-events"), eq("BTCUSDT"), eventCaptor.capture());
        assertThat(eventCaptor.getValue().getAction()).isEqualTo("RELEASE");
        assertThat(eventCaptor.getValue().getSymbol()).isEqualTo("BTCUSDT");
    }
}
