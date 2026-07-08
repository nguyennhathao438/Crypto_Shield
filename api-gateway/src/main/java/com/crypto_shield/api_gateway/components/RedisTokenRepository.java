package com.crypto_shield.api_gateway.components;

import com.crypto_shield.api_gateway.entity.RedisToken;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RedisTokenRepository extends CrudRepository<RedisToken, String> {
    boolean existsById(String id);

    @Override
    void deleteById(String s);
}