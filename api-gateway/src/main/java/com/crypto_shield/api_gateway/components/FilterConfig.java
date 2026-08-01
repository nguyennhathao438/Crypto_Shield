package com.crypto_shield.api_gateway.components;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Value("${jwt.signerKey}")
    private String signerKey;

    private final RedisTokenRepository redisTokenRepository;

    public FilterConfig(RedisTokenRepository redisTokenRepository) {
        this.redisTokenRepository = redisTokenRepository;
    }

    @Bean
    public JwtAuthFilter jwtAuthFilter() {
        return new JwtAuthFilter(signerKey, redisTokenRepository);
    }
}