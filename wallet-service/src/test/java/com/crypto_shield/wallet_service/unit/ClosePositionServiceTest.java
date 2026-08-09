package com.crypto_shield.wallet_service.unit;

import com.crypto_shield.wallet_service.dto.request.ClosePositionRequest;
import com.crypto_shield.wallet_service.dto.response.ClosePositionResponse;
import com.crypto_shield.wallet_service.entity.Position;
import com.crypto_shield.wallet_service.entity.Wallet;
import com.crypto_shield.wallet_service.enums.ErrorCode;
import com.crypto_shield.wallet_service.enums.PositionSide;
import com.crypto_shield.wallet_service.enums.PositionStatus;
import com.crypto_shield.wallet_service.exception.AppException;
import com.crypto_shield.wallet_service.repository.PositionRepository;
import com.crypto_shield.wallet_service.repository.WalletRepository;
import com.crypto_shield.wallet_service.service.ClosePositionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClosePositionService Unit Tests")
class ClosePositionServiceTest {

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private WalletRepository walletRepository;

    private ClosePositionService closePositionService;
    private UUID userId;
    private UUID walletId;
    private UUID positionId;
    private Wallet wallet;
    private Position position;

    @BeforeEach
    void setUp() {
        closePositionService = new ClosePositionService(positionRepository, walletRepository);
        userId = UUID.randomUUID();
        walletId = UUID.randomUUID();
        positionId = UUID.randomUUID();
        wallet = Wallet.builder()
                .id(walletId)
                .userId(userId)
                .balance(new BigDecimal("900"))
                .lockBalance(new BigDecimal("100"))
                .build();
        position = Position.builder()
                .id(positionId)
                .walletId(walletId)
                .symbol("BTCUSDT")
                .side(PositionSide.LONG)
                .quantity(new BigDecimal("2"))
                .averageEntryPrice(new BigDecimal("100"))
                .leverage(2)
                .margin(new BigDecimal("100"))
                .liquidationPrice(new BigDecimal("50"))
                .status(PositionStatus.OPEN)
                .build();
    }

    @Test
    @DisplayName("Should fully close long position at entry price")
    void closePosition_fullLongAtEntryPrice_closesPositionAndUnlocksMargin() {
        // Given
        ClosePositionRequest request = closeRequest(new BigDecimal("2"), new BigDecimal("100"));
        mockWalletAndPosition();

        // When
        ClosePositionResponse result = closePositionService.closePosition(request);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getRealizedPnl()).isEqualByComparingTo(BigDecimal.ZERO.setScale(8));
        assertThat(result.getPosition().getQuantity()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getPosition().getMargin()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getPosition().getLiquidationPrice()).isNull();
        assertThat(result.getPosition().getStatus()).isEqualTo("CLOSED");
        assertThat(result.getWallet().getBalance()).isEqualByComparingTo(new BigDecimal("1000.00000000"));
        assertThat(result.getWallet().getLockBalance()).isEqualByComparingTo(new BigDecimal("0E-8"));
        verify(walletRepository).save(wallet);
        verify(positionRepository).save(position);
    }

    @Test
    @DisplayName("Should partially close short position at entry price")
    void closePosition_partialShortAtEntryPrice_reducesQuantityAndMargin() {
        // Given
        position.setSide(PositionSide.SHORT);
        ClosePositionRequest request = closeRequest(new BigDecimal("0.5"), new BigDecimal("100"));
        mockWalletAndPosition();

        // When
        ClosePositionResponse result = closePositionService.closePosition(request);

        // Then
        assertThat(result.getClosedQuantity()).isEqualByComparingTo(new BigDecimal("0.5"));
        assertThat(result.getRealizedPnl()).isEqualByComparingTo(BigDecimal.ZERO.setScale(8));
        assertThat(result.getPosition().getQuantity()).isEqualByComparingTo(new BigDecimal("1.5"));
        assertThat(result.getPosition().getMargin()).isEqualByComparingTo(new BigDecimal("75.00000000"));
        assertThat(result.getPosition().getLiquidationPrice()).isEqualByComparingTo(new BigDecimal("50"));
        assertThat(result.getPosition().getStatus()).isEqualTo("OPEN");
        assertThat(result.getWallet().getBalance()).isEqualByComparingTo(new BigDecimal("925.00000000"));
        assertThat(result.getWallet().getLockBalance()).isEqualByComparingTo(new BigDecimal("75.00000000"));
    }

    @Test
    @DisplayName("Should reject when wallet is missing")
    void closePosition_walletMissing_throwsHasNotWallet() {
        // Given
        ClosePositionRequest request = closeRequest(BigDecimal.ONE, new BigDecimal("100"));
        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.empty());

        // When / Then
        assertAppException(() -> closePositionService.closePosition(request), ErrorCode.HAS_NOT_WALLET);
        verify(positionRepository, never()).findByIdForUpdate(any());
    }

    @Test
    @DisplayName("Should reject when position is missing")
    void closePosition_positionMissing_throwsPositionNotFound() {
        // Given
        ClosePositionRequest request = closeRequest(BigDecimal.ONE, new BigDecimal("100"));
        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));
        when(positionRepository.findByIdForUpdate(positionId)).thenReturn(Optional.empty());

        // When / Then
        assertAppException(() -> closePositionService.closePosition(request), ErrorCode.POSITION_NOT_FOUND);
    }

    @Test
    @DisplayName("Should reject position from another wallet")
    void closePosition_positionBelongsToAnotherWallet_throwsForbidden() {
        // Given
        ClosePositionRequest request = closeRequest(BigDecimal.ONE, new BigDecimal("100"));
        position.setWalletId(UUID.randomUUID());
        mockWalletAndPosition();

        // When / Then
        assertAppException(() -> closePositionService.closePosition(request), ErrorCode.POSITION_NOT_BELONG_TO_WALLET);
    }

    @Test
    @DisplayName("Should reject already closed position")
    void closePosition_positionAlreadyClosed_throwsAlreadyClosed() {
        // Given
        ClosePositionRequest request = closeRequest(BigDecimal.ONE, new BigDecimal("100"));
        position.setStatus(PositionStatus.CLOSED);
        mockWalletAndPosition();

        // When / Then
        assertAppException(() -> closePositionService.closePosition(request), ErrorCode.POSITION_ALREADY_CLOSED);
    }

    @Test
    @DisplayName("Should reject closing more than available quantity")
    void closePosition_quantityExceedsPosition_throwsQuantityExceedsPosition() {
        // Given
        ClosePositionRequest request = closeRequest(new BigDecimal("2.01"), new BigDecimal("100"));
        mockWalletAndPosition();

        // When / Then
        assertAppException(() -> closePositionService.closePosition(request), ErrorCode.CLOSE_QUANTITY_EXCEEDS_POSITION);
    }

    @Test
    @DisplayName("Should surface dependency exception from position save")
    void closePosition_positionRepositorySaveThrows_propagatesException() {
        // Given
        ClosePositionRequest request = closeRequest(new BigDecimal("2"), new BigDecimal("100"));
        RuntimeException databaseException = new RuntimeException("optimistic lock failure");
        mockWalletAndPosition();
        when(positionRepository.save(position)).thenThrow(databaseException);

        // When / Then
        assertThatThrownBy(() -> closePositionService.closePosition(request))
                .isSameAs(databaseException);
    }

    private void mockWalletAndPosition() {
        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));
        when(positionRepository.findByIdForUpdate(positionId)).thenReturn(Optional.of(position));
    }

    private ClosePositionRequest closeRequest(BigDecimal closedQuantity, BigDecimal currentPrice) {
        return ClosePositionRequest.builder()
                .userId(userId)
                .positionId(positionId)
                .symbol("BTCUSDT")
                .closedQuantity(closedQuantity)
                .currentPrice(currentPrice)
                .build();
    }

    private void assertAppException(Runnable runnable, ErrorCode errorCode) {
        assertThatThrownBy(runnable::run)
                .isInstanceOf(AppException.class)
                .satisfies(exception -> assertThat(((AppException) exception).getErrorCode()).isEqualTo(errorCode));
    }
}
