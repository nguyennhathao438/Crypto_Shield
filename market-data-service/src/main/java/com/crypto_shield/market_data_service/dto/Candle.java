package com.crypto_shield.market_data_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Candle {
    private Long openTime;
    private String open;
    private String high;
    private String low;
    private String close;
    private String volume;
    private Long closeTime;
    private String quoteAssetVolume;
    private Integer numberOfTrades;
    private String takerBuyBaseVolume;
    private String takerBuyQuoteVolume;
}
