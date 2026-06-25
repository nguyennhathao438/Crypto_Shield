package com.cryptoshield.cryptoshield.user.dto;

import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.util.concurrent.TimeUnit;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("RedisHash")
@Builder
public class RedisToken {
    @Id
    private String jwtId;
    @TimeToLive
    private Long expiredTime;
}
