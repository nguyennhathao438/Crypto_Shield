package com.crypto_shield.wallet_service.unit;

import com.crypto_shield.wallet_service.component.PositionTriggerCommandProducer;
import com.crypto_shield.wallet_service.dto.request.OpenPositionRequest;
import com.crypto_shield.wallet_service.dto.response.OpenPositionResponse;
import com.crypto_shield.wallet_service.entity.Position;
import com.crypto_shield.wallet_service.entity.Wallet;
import com.crypto_shield.wallet_service.enums.ErrorCode;
import com.crypto_shield.wallet_service.enums.PositionSide;
import com.crypto_shield.wallet_service.enums.PositionStatus;
import com.crypto_shield.wallet_service.exception.AppException;
import com.crypto_shield.wallet_service.repository.PositionRepository;
import com.crypto_shield.wallet_service.repository.WalletRepository;
import com.crypto_shield.wallet_service.service.WalletPositionService;
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
@DisplayName("WalletPositionService Unit Tests")
class WalletPositionServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private PositionTriggerCommandProducer positionTriggerCommandProducer;

    private WalletPositionService walletPositionService;
    private UUID userId;
    private UUID walletId;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        walletPositionService = new WalletPositionService(walletRepository, positionRepository,positionTriggerCommandProducer);
        userId = UUID.randomUUID();
        walletId = UUID.randomUUID();
        wallet = Wallet.builder()
                .id(walletId)
                .userId(userId)
                .balance(new BigDecimal("1000.00"))
                .lockBalance(BigDecimal.ZERO)
                .build();
    }

    @Test
    @DisplayName("Should create new long position and lock margin")
    void openPosition_validBuyOrder_createsPositionAndLocksMargin() {
        // Given
        OpenPositionRequest request = openRequest("BUY", "1", "100", 10, "10");
        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(positionRepository.findOpenPositionForUpdate(walletId, "BTCUSDT", PositionSide.LONG, PositionStatus.OPEN))
                .thenReturn(Optional.empty());
        when(positionRepository.save(any(Position.class))).thenAnswer(invocation -> {
            Position saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        // When
        OpenPositionResponse result = walletPositionService.openPosition(request);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getPosition().getSide()).isEqualTo("LONG");
        assertThat(result.getPosition().getLiquidationPrice()).isEqualByComparingTo(new BigDecimal("90.00000000"));
        assertThat(result.getWallet().getBalance()).isEqualByComparingTo(new BigDecimal("990.00"));
        assertThat(result.getWallet().getLockBalance()).isEqualByComparingTo(new BigDecimal("10"));
        verify(walletRepository).save(wallet);
        verify(positionRepository).save(any(Position.class));
    }

    @Test
    @DisplayName("Should merge into existing short position")
    void openPosition_existingSellPosition_mergesQuantityAndAveragePrice() {
        // Given
        OpenPositionRequest request = openRequest("SELL", "2", "120", 10, "24");
        Position existing = Position.builder()
                .id(UUID.randomUUID())
                .walletId(walletId)
                .symbol("BTCUSDT")
                .side(PositionSide.SHORT)
                .quantity(new BigDecimal("1"))
                .averageEntryPrice(new BigDecimal("100"))
                .leverage(5)
                .margin(new BigDecimal("20"))
                .status(PositionStatus.OPEN)
                .build();
        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));
        when(positionRepository.findOpenPositionForUpdate(walletId, "BTCUSDT", PositionSide.SHORT, PositionStatus.OPEN))
                .thenReturn(Optional.of(existing));
        when(positionRepository.save(any(Position.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        OpenPositionResponse result = walletPositionService.openPosition(request);

        // Then
        assertThat(result.getPosition().getQuantity()).isEqualByComparingTo(new BigDecimal("3"));
        assertThat(result.getPosition().getAverageEntryPrice()).isEqualByComparingTo(new BigDecimal("113.33333333"));
        assertThat(result.getPosition().getMargin()).isEqualByComparingTo(new BigDecimal("44"));
        assertThat(result.getPosition().getLiquidationPrice()).isEqualByComparingTo(new BigDecimal("128.00000000"));
        assertThat(result.getWallet().getBalance()).isEqualByComparingTo(new BigDecimal("976.00"));
    }

    @Test
    @DisplayName("Should reject null quantity")
    void openPosition_nullQuantity_throwsInvalidQuantity() {
        // Given
        OpenPositionRequest request = openRequest("BUY", "1", "100", 10, "10");
        request.setQuantity(null);

        // When / Then
        assertAppException(() -> walletPositionService.openPosition(request), ErrorCode.INVALID_QUANTITY);
        verify(walletRepository, never()).findByUserIdForUpdate(any());
    }

    @Test
    @DisplayName("Should reject zero quantity")
    void openPosition_zeroQuantity_throwsInvalidQuantity() {
        // Given
        OpenPositionRequest request = openRequest("BUY", "0", "100", 10, "0");

        // When / Then
        assertAppException(() -> walletPositionService.openPosition(request), ErrorCode.INVALID_QUANTITY);
    }

    @Test
    @DisplayName("Should reject invalid leverage")
    void openPosition_zeroLeverage_throwsInvalidLeverage() {
        // Given
        OpenPositionRequest request = openRequest("BUY", "1", "100", 0, "10");

        // When / Then
        assertAppException(() -> walletPositionService.openPosition(request), ErrorCode.INVALID_LEVERAGE);
    }

    @Test
    @DisplayName("Should reject margin mismatch")
    void openPosition_marginMismatch_throwsMarginMismatch() {
        // Given
        OpenPositionRequest request = openRequest("BUY", "1", "100", 10, "9.98");

        // When / Then
        assertAppException(() -> walletPositionService.openPosition(request), ErrorCode.MARGIN_MISMATCH);
    }

    @Test
    @DisplayName("Should reject when wallet does not exist")
    void openPosition_walletMissing_throwsHasNotWallet() {
        // Given
        OpenPositionRequest request = openRequest("BUY", "1", "100", 10, "10");
        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.empty());

        // When / Then
        assertAppException(() -> walletPositionService.openPosition(request), ErrorCode.HAS_NOT_WALLET);
    }

    @Test
    @DisplayName("Should reject when balance is insufficient")
    void openPosition_insufficientBalance_throwsInsufficientBalance() {
        // Given
        OpenPositionRequest request = openRequest("BUY", "100", "100", 1, "10000");
        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));

        // When / Then
        assertAppException(() -> walletPositionService.openPosition(request), ErrorCode.INSUFFICIENT_BALANCE);
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Should reject unsupported side")
    void openPosition_invalidSide_throwsInvalidSide() {
        // Given
        OpenPositionRequest request = openRequest("HOLD", "1", "100", 10, "10");
        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));

        // When / Then
        assertAppException(() -> walletPositionService.openPosition(request), ErrorCode.INVALID_SIDE);
    }

    @Test
    @DisplayName("Should surface dependency exception from wallet save")
    void openPosition_walletRepositorySaveThrows_propagatesException() {
        // Given
        OpenPositionRequest request = openRequest("BUY", "1", "100", 10, "10");
        RuntimeException databaseException = new RuntimeException("database timeout");
        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(wallet)).thenThrow(databaseException);

        // When / Then
        assertThatThrownBy(() -> walletPositionService.openPosition(request))
                .isSameAs(databaseException);
    }

    private OpenPositionRequest openRequest(String side, String quantity, String price, int leverage, String margin) {
        OpenPositionRequest request = new OpenPositionRequest();
        request.setUserId(userId);
        request.setSymbol("BTCUSDT");
        request.setType("MARKET");
        request.setSide(side);
        request.setQuantity(new BigDecimal(quantity));
        request.setPrice(new BigDecimal(price));
        request.setLeverage(leverage);
        request.setMargin(new BigDecimal(margin));
        return request;
    }

    private void assertAppException(Runnable runnable, ErrorCode errorCode) {
        assertThatThrownBy(runnable::run)
                .isInstanceOf(AppException.class)
                .satisfies(exception -> assertThat(((AppException) exception).getErrorCode()).isEqualTo(errorCode));
    }
}
