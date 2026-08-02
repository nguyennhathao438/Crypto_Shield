package com.crypto_shield.wallet_service.service;

import com.crypto_shield.wallet_service.dto.response.CheckBalanceResponse;
import com.crypto_shield.wallet_service.dto.response.WalletResponse;
import com.crypto_shield.wallet_service.entity.Wallet;
import com.crypto_shield.wallet_service.exception.AppException;
import com.crypto_shield.wallet_service.enums.ErrorCode;
import com.crypto_shield.wallet_service.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class WalletService {
    @Autowired
    WalletRepository walletRepository;
    public WalletResponse createWallet(UUID user_Id){
        if(walletRepository.existsByUserId(user_Id)){
            throw new AppException(ErrorCode.HAS_HAVE_WALLET);
        }
        Wallet wallet = Wallet.builder()
                .balance(BigDecimal.valueOf(1000))
                .lockBalance(BigDecimal.valueOf(0))
                .userId(user_Id)
                .build();
        walletRepository.save(wallet);
        return WalletResponse.builder()
                .balance(wallet.getBalance())
                .blockBalance(wallet.getLockBalance())
                .build();
    }
    public WalletResponse getWalletByUser(UUID user_Id){
        Wallet wallet = walletRepository.findByUserId(user_Id).orElseThrow(()->new AppException(ErrorCode.HAS_NOT_WALLET));
        return WalletResponse.builder()
                .balance(wallet.getBalance())
                .blockBalance(wallet.getLockBalance())
                .build();
    }
    public CheckBalanceResponse checkBalance(UUID user_Id, BigDecimal margin){
        Wallet wallet = walletRepository.findByUserId(user_Id).orElseThrow(()->new AppException(ErrorCode.HAS_NOT_WALLET));
        if(wallet.getBalance().compareTo(margin) <0){
            return CheckBalanceResponse.builder()
                    .success(false)
                    .message("Insufficient balance")
                    .build();
        }
        return CheckBalanceResponse.builder()
                .success(true)
                .message("enough balance")
                .build();
    }
}
