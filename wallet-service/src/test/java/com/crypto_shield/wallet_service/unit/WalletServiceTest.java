package com.crypto_shield.wallet_service.unit;

import com.crypto_shield.wallet_service.dto.response.WalletResponse;
import com.crypto_shield.wallet_service.dto.response.CheckBalanceResponse;
import com.crypto_shield.wallet_service.entity.Wallet;
import com.crypto_shield.wallet_service.exception.AppException;
import com.crypto_shield.wallet_service.enums.ErrorCode;
import com.crypto_shield.wallet_service.repository.WalletRepository;
import com.crypto_shield.wallet_service.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WalletService Unit Tests")
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private WalletService walletService;

    private UUID testUserId;
    private Wallet testWallet;
    private WalletResponse expectedResponse;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testWallet = Wallet.builder()
                .id(UUID.randomUUID())
                .userId(testUserId)
                .balance(BigDecimal.valueOf(1000))
                .lockBalance(BigDecimal.valueOf(0))
                .build();
        expectedResponse = WalletResponse.builder()
                .balance(BigDecimal.valueOf(1000))
                .blockBalance(BigDecimal.valueOf(0))
                .build();
    }

    @Test
    @DisplayName("Should create wallet successfully when user has no wallet")
    void createWallet_Success() {
        // Arrange
        when(walletRepository.existsByUserId(testUserId)).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);

        // Act
        WalletResponse result = walletService.createWallet(testUserId);

        // Assert
        assertThat(result)
                .isNotNull()
                .satisfies(response -> {
                    assertThat(response.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(1000));
                    assertThat(response.getBlockBalance()).isEqualByComparingTo(BigDecimal.valueOf(0));
                });

        verify(walletRepository, times(1)).existsByUserId(testUserId);
        verify(walletRepository, times(1)).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Should throw AppException when user already has a wallet")
    void createWallet_UserAlreadyHasWallet() {
        // Arrange
        when(walletRepository.existsByUserId(testUserId)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> walletService.createWallet(testUserId))
                .isInstanceOf(AppException.class)
                .satisfies(exception -> {
                    AppException appException = (AppException) exception;
                    assertThat(appException.getErrorCode()).isEqualTo(ErrorCode.HAS_HAVE_WALLET);
                });

        verify(walletRepository, times(1)).existsByUserId(testUserId);
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Should retrieve wallet successfully when wallet exists")
    void getWalletByUser_Success() {
        // Arrange
        when(walletRepository.findByUserId(testUserId)).thenReturn(Optional.of(testWallet));

        // Act
        WalletResponse result = walletService.getWalletByUser(testUserId);

        // Assert
        assertThat(result)
                .isNotNull()
                .satisfies(response -> {
                    assertThat(response.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(1000));
                    assertThat(response.getBlockBalance()).isEqualByComparingTo(BigDecimal.valueOf(0));
                });

        verify(walletRepository, times(1)).findByUserId(testUserId);
    }

    @Test
    @DisplayName("Should throw AppException when wallet does not exist for user")
    void getWalletByUser_WalletNotFound() {
        // Arrange
        when(walletRepository.findByUserId(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> walletService.getWalletByUser(testUserId))
                .isInstanceOf(AppException.class)
                .satisfies(exception -> {
                    AppException appException = (AppException) exception;
                    assertThat(appException.getErrorCode()).isEqualTo(ErrorCode.HAS_NOT_WALLET);
                });

        verify(walletRepository, times(1)).findByUserId(testUserId);
    }

    @Test
    @DisplayName("Should return success when wallet balance equals requested margin")
    void checkBalance_balanceEqualsMargin_returnsEnoughBalance() {
        // Given
        when(walletRepository.findByUserId(testUserId)).thenReturn(Optional.of(testWallet));

        // When
        CheckBalanceResponse result = walletService.checkBalance(testUserId, BigDecimal.valueOf(1000));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo("enough balance");
        verify(walletRepository).findByUserId(testUserId);
    }

    @Test
    @DisplayName("Should return success when wallet balance is greater than requested margin")
    void checkBalance_balanceGreaterThanMargin_returnsEnoughBalance() {
        // Given
        when(walletRepository.findByUserId(testUserId)).thenReturn(Optional.of(testWallet));

        // When
        CheckBalanceResponse result = walletService.checkBalance(testUserId, BigDecimal.valueOf(250));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo("enough balance");
    }

    @Test
    @DisplayName("Should return failure when wallet balance is lower than requested margin")
    void checkBalance_balanceLowerThanMargin_returnsInsufficientBalance() {
        // Given
        when(walletRepository.findByUserId(testUserId)).thenReturn(Optional.of(testWallet));

        // When
        CheckBalanceResponse result = walletService.checkBalance(testUserId, BigDecimal.valueOf(1000.01));

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Insufficient balance");
    }

    @Test
    @DisplayName("Should throw AppException when checking balance for missing wallet")
    void checkBalance_walletNotFound_throwsAppException() {
        // Given
        when(walletRepository.findByUserId(testUserId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> walletService.checkBalance(testUserId, BigDecimal.ONE))
                .isInstanceOf(AppException.class)
                .satisfies(exception -> assertThat(((AppException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.HAS_NOT_WALLET));
    }
}
