package com.crypto_shield.market_data_service.dto;

import lombok.Data;

@Data
public class KlineRequest {
    private String symbol;
    private String interval;
    private Long startTime;
    private Long endTime;
    private Integer limit;
}
