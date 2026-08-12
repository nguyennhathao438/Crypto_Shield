package com.cryptoshield.order_service.components;

import com.cryptoshield.order_service.dto.PositionTrigger;
import com.cryptoshield.order_service.enums.PositionSide;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@RequiredArgsConstructor
@Slf4j
public class PositionTriggerEngine {
    private final Map<String, NavigableMap<BigDecimal, List<PositionTrigger>>> longTriggers = new ConcurrentHashMap<>();
    private final Map<String, NavigableMap<BigDecimal, List<PositionTrigger>>> shortTriggers = new ConcurrentHashMap<>();
    private final SymbolDemandProducer symbolDemandProducer;
    private final PositionTriggerFiredProducer firedProducer;

    private final WebClient walletServiceWebClient;
    @PostConstruct
    public void restoreState() {
        List<PositionTrigger> active = getActiveTriggers();

        if (active == null) {
            log.error("KHÔNG lấy được active triggers từ wallet-service sau khi retry — " +
                    "order-service khởi động THIẾU DATA liquidation, cần kiểm tra ngay!");
            return;
        }

        active.forEach(dto -> {
            registerInMemory(toModel(dto));
            symbolDemandProducer.requestSymbol(dto.getSymbol());
            log.info("Restored trigger: positionId={}, symbol={}, side={}, triggerPrice={}",
                    dto.getPositionId(), dto.getSymbol(), dto.getSide(), dto.getTriggerPrice());
        });
        log.info("Khôi phục {} liquidation trigger từ wallet-service", active.size());
    }
    public void register(PositionTrigger trigger) {
        registerInMemory(trigger);
        symbolDemandProducer.requestSymbol(trigger.getSymbol());
    }

    private void registerInMemory(PositionTrigger trigger) {
        Map<String, NavigableMap<BigDecimal, List<PositionTrigger>>> target = mapFor(trigger.getSide());
        target.computeIfAbsent(trigger.getSymbol(), s -> new ConcurrentSkipListMap<>())
                .computeIfAbsent(trigger.getTriggerPrice(), p -> new CopyOnWriteArrayList<>())
                .add(trigger);
    }
    private Map<String, NavigableMap<BigDecimal, List<PositionTrigger>>> mapFor(PositionSide side) {
        return side == PositionSide.LONG ? longTriggers : shortTriggers;
    }
    public List<PositionTrigger> getActiveTriggers() {
        try {
            return getActiveTriggersMono().block();
        } catch (Exception e) {
            log.error("Gọi wallet-service thất bại hoàn toàn sau khi retry hết", e);
            return null;
        }
    }
    private PositionTrigger toModel(PositionTrigger dto) {
        return new PositionTrigger(dto.getPositionId(), dto.getSymbol(), dto.getTriggerPrice(), dto.getSide());
    }
    public void cancel(UUID positionId, String symbol, BigDecimal liquidationPrice, PositionSide side) {
        Map<String, NavigableMap<BigDecimal, List<PositionTrigger>>> target = mapFor(side);
        var priceMap = target.get(symbol);
        if (priceMap == null) return;

        var list = priceMap.get(liquidationPrice);
        if (list != null) {
            list.removeIf(t -> t.getPositionId().equals(positionId));
            if (list.isEmpty()) priceMap.remove(liquidationPrice);
        }
    }

    public void checkTriggers(String symbol, BigDecimal currentPrice) {
        log.info("checkTriggers called: symbol={}, currentPrice={}, longTriggersHasSymbol={}, shortTriggersHasSymbol={}",
                symbol, currentPrice, longTriggers.containsKey(symbol), shortTriggers.containsKey(symbol));

        var longMap = longTriggers.get(symbol);
        if (longMap != null && !longMap.isEmpty()) {
            // LONG: liquidate khi currentPrice <= triggerPrice → lấy trigger có key >= currentPrice
            var candidates = longMap.tailMap(currentPrice, true);
            if (!candidates.isEmpty()) {
                fireAndClear(symbol, candidates, currentPrice);
            }
            if (longMap.isEmpty()) {
                longTriggers.remove(symbol);
                maybeUnsubscribe(symbol);
            }
        }

        var shortMap = shortTriggers.get(symbol);
        if (shortMap != null && !shortMap.isEmpty()) {

            // SHORT: liquidate khi currentPrice >= triggerPrice → lấy trigger có key <= currentPrice
            var candidates = shortMap.headMap(currentPrice, true);
            if (!candidates.isEmpty()) {
                fireAndClear(symbol, candidates, currentPrice);
            }
            if (shortMap.isEmpty()) {
                shortTriggers.remove(symbol);
                maybeUnsubscribe(symbol);
            }
        }
    }

    private void fireAndClear(String symbol, NavigableMap<BigDecimal, List<PositionTrigger>> triggeredMap,
                              BigDecimal currentPrice) {
        log.info("Kích hoạt thanh lý cho symbol={}: tìm thấy {} mức giá thỏa điều kiện tại currentPrice={}",
                symbol, triggeredMap.size(), currentPrice);

        var iterator = triggeredMap.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            List<PositionTrigger> triggers = entry.getValue();
            List<PositionTrigger> toRemove = new ArrayList<>();

            for (PositionTrigger trigger : triggers) {
                try {
                    boolean sent = firedProducer.publish(trigger, currentPrice);
                    if (sent) {
                        toRemove.add(trigger);
                    } else {
                        log.error("Giữ lại trigger {} để thử publish lại ở tick giá sau", trigger.getPositionId());
                    }
                } catch (Exception e) {
                    log.error("LỖI EXCEPTION khi publish trigger thanh lý cho positionId={}", trigger.getPositionId(), e);
                }
            }

            triggers.removeAll(toRemove);
            if (triggers.isEmpty()) {
                iterator.remove(); // xoá price-level; vì headMap/tailMap là view nên tự phản ánh vào map gốc
            }
        }
    }
    private void maybeUnsubscribe(String symbol) {
        boolean stillNeeded = longTriggers.containsKey(symbol) || shortTriggers.containsKey(symbol);
        if (!stillNeeded) {
            symbolDemandProducer.releaseSymbol(symbol);
            log.info("Không còn trigger nào cho symbol {}, đã bỏ theo dõi", symbol);
        }
    }
    public Mono<List<PositionTrigger>> getActiveTriggersMono() {
        return walletServiceWebClient.get()
                .uri("/internal/position/active-triggers")
                .retrieve()
                .bodyToFlux(PositionTrigger.class)
                .collectList()
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .doBeforeRetry(sig -> log.warn("Retry gọi wallet-service lần {}", sig.totalRetries() + 1)))
                .doOnError(e -> log.error("Gọi wallet-service thất bại sau khi retry hết", e));
    }

}
