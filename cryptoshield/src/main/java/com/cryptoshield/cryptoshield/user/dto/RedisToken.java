package com.cryptoshield.cryptoshield.user.dto;

import lombok.*;
import org.springframework.data.annotation.Id;
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
