package com.crypto_shield.market_data_service.component;

public interface BinanceCommandSender {
    void sendSubscribeCommand(String stream, String method);
}
