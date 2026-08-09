package com.crypto_shield.market_data_service.component;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SymbolSubscriptionManager {
    final PriceCache priceCache;
    BinanceCommandSender commandSender;

    final Set<String> subscribedSymbols = ConcurrentHashMap.newKeySet();
    SinkCleaner sinkCleaner;
    final ConcurrentHashMap<String, AtomicInteger> businessDemandCount = new ConcurrentHashMap<>();
    final ConcurrentHashMap<String, AtomicInteger> streamClientCount = new ConcurrentHashMap<>();
    public void setCommandSender(BinanceCommandSender sender) {
        this.commandSender = sender;
    }
    public void setSinkCleaner(SinkCleaner cleaner) {
        this.sinkCleaner = cleaner;
    }
    // ============ NGUỒN 1: Business demand (gọi từ Kafka Consumer khi nhận NEED/RELEASE) ============

    public void onBusinessDemandIncrease(String symbol) {
        String upper = symbol.toUpperCase();
        businessDemandCount.computeIfAbsent(upper, s -> new AtomicInteger(0)).incrementAndGet();
        ensureSubscribed(upper);
        log.info("Business demand tăng cho {}: count={}", upper, businessDemandCount.get(upper).get());
    }

    public void onBusinessDemandDecrease(String symbol) {
        String upper = symbol.toUpperCase();
        AtomicInteger count = businessDemandCount.get(upper);
        if (count != null) {
            int remaining = count.decrementAndGet();
            log.info("Business demand giảm cho {}: còn {}", upper, remaining);
        }
        evaluateUnsubscribe(upper);
    }
    // ============ NGUỒN 2: Client streaming (gọi từ Controller khi FE connect/disconnect) ============

    public void onStreamClientConnect(String symbol) {
        String upper = symbol.toUpperCase();
        streamClientCount.computeIfAbsent(upper, s -> new AtomicInteger(0)).incrementAndGet();
        ensureSubscribed(upper);
        log.info("Stream client tăng cho {}: count={}", upper, streamClientCount.get(upper).get());
    }

    public void onStreamClientDisconnect(String symbol) {
        String upper = symbol.toUpperCase();
        AtomicInteger count = streamClientCount.get(upper);
        log.info(">>> onStreamClientDisconnect được gọi cho {}", symbol);
        int remaining = 0;
        if (count != null) {
            remaining = count.decrementAndGet();
            log.info("Stream client giảm cho {}: còn {}", upper, remaining);
        }
        if (remaining <= 0 && sinkCleaner != null) {
            sinkCleaner.removeSinkIfUnused(upper);
        }
        evaluateUnsubscribe(upper);
    }

    // ============ LOGIC DÙNG CHUNG ============

    /** Chỉ gọi Binance SUBSCRIBE nếu symbol CHƯA có trong danh sách - dù nguồn nào gọi vào */
    private void ensureSubscribed(String symbol) {
        if (subscribedSymbols.add(symbol)) {
            commandSender.sendSubscribeCommand(symbol.toLowerCase() + "@ticker", "SUBSCRIBE");
            log.info("[Binance] Subscribe MỚI cho {}", symbol);
        }
    }

    /** Chỉ unsubscribe khi CẢ HAI nguồn cùng về 0 */
    private void evaluateUnsubscribe(String symbol) {
        int businessCount = getCount(businessDemandCount, symbol);
        int streamCount = getCount(streamClientCount, symbol);
        log.info("Evaluate unsubscribe {}: business={}, stream={}", symbol, businessCount, streamCount);
        if (businessCount <= 0 && streamCount <= 0) {
            if (subscribedSymbols.remove(symbol)) {
                commandSender.sendSubscribeCommand(symbol.toLowerCase() + "@ticker", "UNSUBSCRIBE");
                priceCache.remove(symbol);
                businessDemandCount.remove(symbol);
                streamClientCount.remove(symbol);
                log.info("[Binance] Unsubscribe {} - không còn ai cần (business=0, stream=0)", symbol);
            }
        }
    }

    private int getCount(ConcurrentHashMap<String, AtomicInteger> map, String symbol) {
        AtomicInteger counter = map.get(symbol);
        return counter != null ? counter.get() : 0;
    }

    public void resubscribeAll() {
        subscribedSymbols.forEach(symbol ->
                commandSender.sendSubscribeCommand(symbol.toLowerCase() + "@ticker", "SUBSCRIBE"));
    }

}
