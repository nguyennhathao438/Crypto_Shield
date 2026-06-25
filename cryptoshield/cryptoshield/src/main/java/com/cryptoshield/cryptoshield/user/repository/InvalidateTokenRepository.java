package com.cryptoshield.cryptoshield.user.repository;

import com.cryptoshield.cryptoshield.user.entity.InvalidateToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvalidateTokenRepository extends JpaRepository<InvalidateToken,String> {
    Optional<InvalidateToken> findById(String s);
}
