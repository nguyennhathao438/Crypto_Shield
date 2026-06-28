package com.crypto_shield.wallet_service.integration;

import com.crypto_shield.wallet_service.dto.WalletResponse;
import com.crypto_shield.wallet_service.exception.AppException;
import com.crypto_shield.wallet_service.exception.ErrorCode;
import com.crypto_shield.wallet_service.service.WalletService;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DisplayName("WalletController Integration Tests")
class WalletControllerIT {

    @Autowired
    private MockMvc mockMvc;



    @MockitoBean
    private WalletService walletService;

    private UUID testUserId;
    private WalletResponse testWalletResponse;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testWalletResponse = WalletResponse.builder()
                .balance(BigDecimal.valueOf(1000))
                .unrealizedPnl(BigDecimal.valueOf(0))
                .build();
    }

    @Test
    @DisplayName("POST /internal/wallets/{userId} - Should return 200 and wallet response on successful creation")
    void createWallet_Success() throws Exception {
        // Arrange
        when(walletService.createWallet(testUserId)).thenReturn(testWalletResponse);

        // Act
        ResultActions result = mockMvc.perform(post("/internal/wallets/{userId}", testUserId)
                .contentType("application/json"));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.result", notNullValue()))
                .andExpect(jsonPath("$.result.balance", is(1000)))
                .andExpect(jsonPath("$.result.unrealizedPnl", is(0)));
    }

    @Test
    @DisplayName("POST /internal/wallets/{userId} - Should return 409 when user already has wallet")
    void createWallet_UserAlreadyHasWallet() throws Exception {
        // Arrange
        AppException exception = new AppException(ErrorCode.HAS_HAVE_WALLET);
        when(walletService.createWallet(testUserId)).thenThrow(exception);

        // Act
        ResultActions result = mockMvc.perform(post("/internal/wallets/{userId}", testUserId)
                .contentType("application/json"));

        // Assert
        result.andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /internal/wallets/users/{userId} - Should return 200 and wallet response on successful retrieval")
    void getWalletByUserId_Success() throws Exception {
        // Arrange
        when(walletService.getWalletByUser(testUserId)).thenReturn(testWalletResponse);

        // Act
        ResultActions result = mockMvc.perform(get("/internal/wallets/users/{userId}", testUserId)
                .contentType("application/json"));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.result", notNullValue()))
                .andExpect(jsonPath("$.result.balance", is(1000)))
                .andExpect(jsonPath("$.result.unrealizedPnl", is(0)));
    }

    @Test
    @DisplayName("GET /internal/wallets/users/{userId} - Should return 404 when wallet not found")
    void getWalletByUserId_WalletNotFound() throws Exception {
        // Arrange
        AppException exception = new AppException(ErrorCode.HAS_NOT_WALLET);
        when(walletService.getWalletByUser(testUserId)).thenThrow(exception);

        // Act
        ResultActions result = mockMvc.perform(get("/internal/wallets/users/{userId}", testUserId)
                .contentType("application/json"));

        // Assert
        result.andExpect(status().isNotFound());
    }
}
