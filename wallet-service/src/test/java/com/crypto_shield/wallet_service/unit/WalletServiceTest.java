package com.crypto_shield.wallet_service.unit;

import com.crypto_shield.wallet_service.dto.WalletResponse;
import com.crypto_shield.wallet_service.entity.Wallet;
import com.crypto_shield.wallet_service.exception.AppException;
import com.crypto_shield.wallet_service.exception.ErrorCode;
import com.crypto_shield.wallet_service.repository.WalletReposiory;
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
    private WalletReposiory walletReposiory;

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
                .unrealizedPnl(BigDecimal.valueOf(0))
                .build();
        expectedResponse = WalletResponse.builder()
                .balance(BigDecimal.valueOf(1000))
                .unrealizedPnl(BigDecimal.valueOf(0))
                .build();
    }

    @Test
    @DisplayName("Should create wallet successfully when user has no wallet")
    void createWallet_Success() {
        // Arrange
        when(walletReposiory.existsByUserId(testUserId)).thenReturn(false);
        when(walletReposiory.save(any(Wallet.class))).thenReturn(testWallet);

        // Act
        WalletResponse result = walletService.createWallet(testUserId);

        // Assert
        assertThat(result)
                .isNotNull()
                .satisfies(response -> {
                    assertThat(response.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(1000));
                    assertThat(response.getUnrealizedPnl()).isEqualByComparingTo(BigDecimal.valueOf(0));
                });

        verify(walletReposiory, times(1)).existsByUserId(testUserId);
        verify(walletReposiory, times(1)).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Should throw AppException when user already has a wallet")
    void createWallet_UserAlreadyHasWallet() {
        // Arrange
        when(walletReposiory.existsByUserId(testUserId)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> walletService.createWallet(testUserId))
                .isInstanceOf(AppException.class)
                .satisfies(exception -> {
                    AppException appException = (AppException) exception;
                    assertThat(appException.getErrorCode()).isEqualTo(ErrorCode.HAS_HAVE_WALLET);
                });

        verify(walletReposiory, times(1)).existsByUserId(testUserId);
        verify(walletReposiory, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Should retrieve wallet successfully when wallet exists")
    void getWalletByUser_Success() {
        // Arrange
        when(walletReposiory.findByUserId(testUserId)).thenReturn(Optional.of(testWallet));

        // Act
        WalletResponse result = walletService.getWalletByUser(testUserId);

        // Assert
        assertThat(result)
                .isNotNull()
                .satisfies(response -> {
                    assertThat(response.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(1000));
                    assertThat(response.getUnrealizedPnl()).isEqualByComparingTo(BigDecimal.valueOf(0));
                });

        verify(walletReposiory, times(1)).findByUserId(testUserId);
    }

    @Test
    @DisplayName("Should throw AppException when wallet does not exist for user")
    void getWalletByUser_WalletNotFound() {
        // Arrange
        when(walletReposiory.findByUserId(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> walletService.getWalletByUser(testUserId))
                .isInstanceOf(AppException.class)
                .satisfies(exception -> {
                    AppException appException = (AppException) exception;
                    assertThat(appException.getErrorCode()).isEqualTo(ErrorCode.HAS_NOT_WALLET);
                });

        verify(walletReposiory, times(1)).findByUserId(testUserId);
    }
}
