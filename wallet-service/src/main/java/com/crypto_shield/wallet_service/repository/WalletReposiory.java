package com.crypto_shield.wallet_service.repository;

import com.crypto_shield.wallet_service.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletReposiory extends JpaRepository<Wallet, UUID> {
    boolean existsByUserId(UUID uuid);
    Optional<Wallet> findByUserId(UUID uuid);
}
