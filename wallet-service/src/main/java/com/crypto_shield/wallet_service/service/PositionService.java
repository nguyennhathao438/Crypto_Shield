package com.crypto_shield.wallet_service.service;

import com.crypto_shield.wallet_service.config.PriceCache;
import com.crypto_shield.wallet_service.config.SymbolDemandPublisher;
import com.crypto_shield.wallet_service.dto.response.PositionResponse;
import com.crypto_shield.wallet_service.entity.Position;
import com.crypto_shield.wallet_service.entity.Wallet;
import com.crypto_shield.wallet_service.enums.ErrorCode;
import com.crypto_shield.wallet_service.enums.PositionSide;
import com.crypto_shield.wallet_service.enums.PositionStatus;
import com.crypto_shield.wallet_service.exception.AppException;
import com.crypto_shield.wallet_service.repository.PositionRepository;
import com.crypto_shield.wallet_service.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PositionService {
    private final PositionRepository positionRepository;
    private final WalletRepository walletRepository;
    private final PriceCache priceCache;
    private final SymbolDemandPublisher symbolDemandPublisher;

    private final ConcurrentHashMap<UUID, Sinks.Many<List<PositionResponse>>> walletSinks = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<UUID, AtomicInteger> walletViewerCount = new ConcurrentHashMap<>();


    public Flux<List<PositionResponse>> streamPositions(UUID userId) {
        Wallet wallet = walletRepository.findByUserId(userId).orElseThrow(()->new AppException(ErrorCode.HAS_NOT_WALLET));
        walletViewerCount.computeIfAbsent(wallet.getId(), w -> new AtomicInteger(0)).incrementAndGet();

        Sinks.Many<List<PositionResponse>> sink = walletSinks.computeIfAbsent(wallet.getId(),
                w -> Sinks.many().multicast().onBackpressureBuffer());

        List<PositionResponse> initialSnapshot = buildPositionResponses(wallet.getId());
        List<String> symbols = initialSnapshot.stream()
                .map(PositionResponse::getSymbol)
                .distinct()
                .toList();
        if (!symbols.isEmpty()) {
            symbolDemandPublisher.publishNeedBatch(symbols);
        }
        return Flux.concat(
                Flux.just(initialSnapshot),
                sink.asFlux()
        ).doFinally(signalType -> onClientDisconnect(wallet.getId(),symbols));
    }

    private void onClientDisconnect(UUID walletId, List<String> symbols) {
        AtomicInteger count = walletViewerCount.get(walletId);
        if (!symbols.isEmpty()) {
            symbolDemandPublisher.publishReleaseBatch(symbols);
        }
        if (count != null && count.decrementAndGet() <= 0) {
            walletSinks.remove(walletId);
            walletViewerCount.remove(walletId);
        }
    }
    public void notifyPriceChanged(String symbol) {
        List<UUID> affectedWalletIds = positionRepository
                .findDistinctWalletIdBySymbolAndStatus(symbol, PositionStatus.OPEN);

        for (UUID walletId : affectedWalletIds) {
            Sinks.Many<List<PositionResponse>> sink = walletSinks.get(walletId);
            if (sink == null) continue;

            List<PositionResponse> updated = buildPositionResponses(walletId);
            sink.tryEmitNext(updated);
        }
    }
    private List<PositionResponse> buildPositionResponses(UUID walletId) {
        List<Position> positions = positionRepository.findByWalletIdAndStatus(walletId, PositionStatus.OPEN);

        return positions.stream()
                .map(this::toPositionResponse)
                .collect(Collectors.toList());
    }

    private PositionResponse toPositionResponse(Position position) {
        Double currentPriceRaw = priceCache.getPrice(position.getSymbol());
        BigDecimal currentPrice = currentPriceRaw != null ? BigDecimal.valueOf(currentPriceRaw) : null;

        BigDecimal unrealizedPnl = null;
        BigDecimal unrealizedPnlPercent = null;

        if (currentPrice != null) {
            unrealizedPnl = calculateUnrealizedPnl(position, currentPrice);
            unrealizedPnlPercent = unrealizedPnl
                    .divide(position.getMargin(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        return PositionResponse.builder()
                .positionId(position.getId())
                .symbol(position.getSymbol())
                .side(position.getSide().name())
                .quantity(position.getQuantity())
                .averageEntryPrice(position.getAverageEntryPrice())
                .currentPrice(currentPrice)
                .leverage(position.getLeverage())
                .margin(position.getMargin())
                .liquidationPrice(position.getLiquidationPrice())
                .unrealizedPnl(unrealizedPnl)
                .unrealizedPnlPercent(unrealizedPnlPercent)
                .build();
    }

    private BigDecimal calculateUnrealizedPnl(Position position, BigDecimal currentPrice) {
        BigDecimal priceDiff = position.getSide() == PositionSide.LONG
                ? currentPrice.subtract(position.getAverageEntryPrice())
                : position.getAverageEntryPrice().subtract(currentPrice);

        return priceDiff.multiply(position.getQuantity()).setScale(8, RoundingMode.HALF_UP);
    }
}
