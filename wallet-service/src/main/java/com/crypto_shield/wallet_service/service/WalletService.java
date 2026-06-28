package com.crypto_shield.wallet_service.service;

import com.crypto_shield.wallet_service.dto.WalletResponse;
import com.crypto_shield.wallet_service.entity.Wallet;
import com.crypto_shield.wallet_service.exception.AppException;
import com.crypto_shield.wallet_service.exception.ErrorCode;
import com.crypto_shield.wallet_service.repository.WalletReposiory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class WalletService {
    @Autowired
    WalletReposiory walletReposiory;
    public WalletResponse createWallet(UUID user_Id){
        if(walletReposiory.existsByUserId(user_Id)){
            throw new AppException(ErrorCode.HAS_HAVE_WALLET);
        }
        Wallet wallet = Wallet.builder()
                .balance(BigDecimal.valueOf(1000))
                .unrealizedPnl(BigDecimal.valueOf(0))
                .userId(user_Id)
                .build();
        walletReposiory.save(wallet);
        return WalletResponse.builder()
                .balance(wallet.getBalance())
                .unrealizedPnl(wallet.getUnrealizedPnl())
                .build();
    }
    public WalletResponse getWalletByUser(UUID user_Id){
        Wallet wallet = walletReposiory.findByUserId(user_Id).orElseThrow(()->new AppException(ErrorCode.HAS_NOT_WALLET));
        return WalletResponse.builder()
                .balance(wallet.getBalance())
                .unrealizedPnl(wallet.getUnrealizedPnl())
                .build();
    }

}
