package com.crypto_shield.wallet_service.controller;

import com.crypto_shield.wallet_service.dto.ApiResponse;
import com.crypto_shield.wallet_service.dto.CheckBalanceResponse;
import com.crypto_shield.wallet_service.dto.WalletResponse;
import com.crypto_shield.wallet_service.service.WalletService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/internal/wallets")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WalletController {
    WalletService walletService;
    @PostMapping("/{userId}")
    public ApiResponse<WalletResponse> createWallet(
            @PathVariable UUID userId) {
        return ApiResponse.<WalletResponse>builder()
                .result(walletService.createWallet(userId))
                .build();
    }
    @GetMapping("/users/{userId}")
    public ApiResponse<WalletResponse> getWalletByUserId(
            @PathVariable UUID userId) {
        return ApiResponse.<WalletResponse>builder()
                .result(walletService.getWalletByUser(userId))
                .build();
    }
    @GetMapping("/balance/{userId}")
    public ApiResponse<CheckBalanceResponse> checkBalance(
            @PathVariable UUID userId,
            @RequestParam BigDecimal margin
    ) {
        return ApiResponse.<CheckBalanceResponse>builder()
                .result(walletService.checkBalance(userId,margin))
                .build();
    }
}
