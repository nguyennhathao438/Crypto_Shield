package com.crypto_shield.wallet_service.config;

import com.crypto_shield.wallet_service.entity.Wallet;
import com.crypto_shield.wallet_service.repository.WalletReposiory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.UUID;

@Configuration
@Slf4j
public class ApplicationConfig {
    @Bean
    ApplicationRunner applicationRunner(WalletReposiory walletReposiory){
        return args -> {
            if (!walletReposiory.existsById(UUID.fromString("11111111-1111-1111-1111-111111111111"))) {
                Wallet wallet = Wallet.builder()
                        .userId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                        .balance(new BigDecimal("1000"))
                        .unrealizedPnl(new BigDecimal("0"))
                        .build();
                walletReposiory.save(wallet);
                log.warn("Create wallet demo success with waletId:11111111-1111-1111-1111-111111111111 userId:22222222-2222-2222-2222-222222222222");
            }
        };
    }
}