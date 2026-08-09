package com.crypto_shield.wallet_service.unit;

import com.crypto_shield.wallet_service.config.PriceCache;
import com.crypto_shield.wallet_service.config.SymbolDemandPublisher;
import com.crypto_shield.wallet_service.dto.response.PositionResponse;
import com.crypto_shield.wallet_service.entity.Position;
import com.crypto_shield.wallet_service.entity.Wallet;
import com.crypto_shield.wallet_service.enums.ErrorCode;
import com.crypto_shield.wallet_service.enums.PositionSide;
import com.crypto_shield.wallet_service.enums.PositionStatus;
import com.crypto_shield.wallet_service.exception.AppException;
import com.crypto_shield.wallet_service.repository.PositionRepository;
import com.crypto_shield.wallet_service.repository.WalletRepository;
import com.crypto_shield.wallet_service.service.PositionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.Disposable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PositionService Unit Tests")
class PositionServiceTest {

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private PriceCache priceCache;

    @Mock
    private SymbolDemandPublisher symbolDemandPublisher;

    private PositionService positionService;
    private UUID userId;
    private UUID walletId;
    private UUID positionId;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        positionService = new PositionService(positionRepository, walletRepository, priceCache, symbolDemandPublisher);
        userId = UUID.randomUUID();
        walletId = UUID.randomUUID();
        positionId = UUID.randomUUID();
        wallet = Wallet.builder()
                .id(walletId)
                .userId(userId)
                .balance(new BigDecimal("900"))
                .lockBalance(new BigDecimal("100"))
                .build();
    }

    @Test
    @DisplayName("Should stream initial positions and publish need and release events")
    void streamPositions_openPositionsWithPrices_emitsInitialSnapshotAndPublishesDemandLifecycle() {
        // Given
        Position position = position(PositionSide.LONG, PositionStatus.OPEN, "100", "2", "100");
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(positionRepository.findByWalletIdAndStatus(walletId, PositionStatus.OPEN)).thenReturn(List.of(position));
        when(priceCache.getPrice("BTCUSDT")).thenReturn(110.0);

        // When
        List<PositionResponse> result = positionService.streamPositions(userId).blockFirst();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUnrealizedPnl()).isEqualByComparingTo(new BigDecimal("20.00000000"));
        assertThat(result.get(0).getUnrealizedPnlPercent()).isEqualByComparingTo(new BigDecimal("20.0000"));
        verify(symbolDemandPublisher).publishNeedBatch(List.of("BTCUSDT"));
        verify(symbolDemandPublisher).publishReleaseBatch(List.of("BTCUSDT"));
    }

    @Test
    @DisplayName("Should stream empty snapshot without publishing symbol demand")
    void streamPositions_noOpenPositions_emitsEmptySnapshotWithoutDemand() {
        // Given
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(positionRepository.findByWalletIdAndStatus(walletId, PositionStatus.OPEN)).thenReturn(List.of());

        // When
        List<PositionResponse> result = positionService.streamPositions(userId).blockFirst();

        // Then
        assertThat(result).isEmpty();
        verify(symbolDemandPublisher, never()).publishNeedBatch(List.of());
        verify(symbolDemandPublisher, never()).publishReleaseBatch(List.of());
    }

    @Test
    @DisplayName("Should reject stream when wallet is missing")
    void streamPositions_walletMissing_throwsHasNotWallet() {
        // Given
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> positionService.streamPositions(userId))
                .isInstanceOf(AppException.class)
                .satisfies(exception -> assertThat(((AppException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.HAS_NOT_WALLET));
    }

    @Test
    @DisplayName("Should notify active stream when matching price changes")
    void notifyPriceChanged_activeSubscriber_emitsUpdatedSnapshot() {
        // Given
        Position initial = position(PositionSide.LONG, PositionStatus.OPEN, "100", "1", "50");
        Position updated = position(PositionSide.LONG, PositionStatus.OPEN, "100", "1", "50");
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(positionRepository.findByWalletIdAndStatus(walletId, PositionStatus.OPEN))
                .thenReturn(List.of(initial), List.of(updated));
        when(priceCache.getPrice("BTCUSDT")).thenReturn(100.0, 120.0);
        when(positionRepository.findDistinctWalletIdBySymbolAndStatus("BTCUSDT", PositionStatus.OPEN))
                .thenReturn(List.of(walletId));
        List<List<PositionResponse>> emissions = new ArrayList<>();

        // When
        Disposable subscription = positionService.streamPositions(userId).subscribe(emissions::add);
        positionService.notifyPriceChanged("BTCUSDT");
        subscription.dispose();

        // Then
        assertThat(emissions).hasSize(2);
        assertThat(emissions.get(1).get(0).getCurrentPrice()).isEqualByComparingTo(new BigDecimal("120.0"));
    }

    @Test
    @DisplayName("Should ignore price changes when there is no stream sink")
    void notifyPriceChanged_noActiveSubscriber_doesNotBuildSnapshot() {
        // Given
        UUID otherWalletId = UUID.randomUUID();
        when(positionRepository.findDistinctWalletIdBySymbolAndStatus("ETHUSDT", PositionStatus.OPEN))
                .thenReturn(List.of(otherWalletId));

        // When
        positionService.notifyPriceChanged("ETHUSDT");

        // Then
        verify(positionRepository, never()).findByWalletIdAndStatus(otherWalletId, PositionStatus.OPEN);
    }

    @Test
    @DisplayName("Should return position by id with short PnL")
    void getPositionById_shortPositionWithPrice_returnsCalculatedPnl() {
        // Given
        Position position = position(PositionSide.SHORT, PositionStatus.OPEN, "100", "2", "50");
        when(positionRepository.findById(positionId)).thenReturn(Optional.of(position));
        when(priceCache.getPrice("BTCUSDT")).thenReturn(90.0);

        // When
        PositionResponse result = positionService.getPositionById(positionId);

        // Then
        assertThat(result.getUnrealizedPnl()).isEqualByComparingTo(new BigDecimal("20.00000000"));
        assertThat(result.getUnrealizedPnlPercent()).isEqualByComparingTo(new BigDecimal("40.0000"));
    }

    @Test
    @DisplayName("Should return position by id without PnL when price is missing")
    void getPositionById_missingCurrentPrice_returnsNullPnl() {
        // Given
        Position position = position(PositionSide.LONG, PositionStatus.OPEN, "100", "2", "50");
        when(positionRepository.findById(positionId)).thenReturn(Optional.of(position));
        when(priceCache.getPrice("BTCUSDT")).thenReturn(null);

        // When
        PositionResponse result = positionService.getPositionById(positionId);

        // Then
        assertThat(result.getCurrentPrice()).isNull();
        assertThat(result.getUnrealizedPnl()).isNull();
        assertThat(result.getUnrealizedPnlPercent()).isNull();
    }

    @Test
    @DisplayName("Should reject missing position id")
    void getPositionById_positionMissing_throwsPositionNotFound() {
        // Given
        when(positionRepository.findById(positionId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> positionService.getPositionById(positionId))
                .isInstanceOf(AppException.class)
                .satisfies(exception -> assertThat(((AppException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.POSITION_NOT_FOUND));
    }

    private Position position(PositionSide side, PositionStatus status, String entryPrice, String quantity, String margin) {
        return Position.builder()
                .id(positionId)
                .walletId(walletId)
                .symbol("BTCUSDT")
                .side(side)
                .quantity(new BigDecimal(quantity))
                .averageEntryPrice(new BigDecimal(entryPrice))
                .leverage(10)
                .margin(new BigDecimal(margin))
                .liquidationPrice(new BigDecimal("50"))
                .status(status)
                .build();
    }
}
