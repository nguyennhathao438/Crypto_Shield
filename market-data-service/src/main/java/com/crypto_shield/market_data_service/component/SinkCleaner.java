package com.crypto_shield.market_data_service.component;

public interface SinkCleaner {
    void removeSinkIfUnused(String symbol);
}
