package com.crypto_shield.wallet_service.service;

import com.crypto_shield.wallet_service.dto.request.ClosePositionRequest;
import com.crypto_shield.wallet_service.dto.response.ClosePositionResponse;
import com.crypto_shield.wallet_service.entity.Position;
import com.crypto_shield.wallet_service.entity.Wallet;
import com.crypto_shield.wallet_service.enums.ErrorCode;
import com.crypto_shield.wallet_service.enums.PositionSide;
import com.crypto_shield.wallet_service.enums.PositionStatus;
import com.crypto_shield.wallet_service.exception.AppException;
import com.crypto_shield.wallet_service.repository.PositionRepository;
import com.crypto_shield.wallet_service.repository.WalletRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClosePositionService {
    private final PositionRepository positionRepository;
    private final WalletRepository walletRepository;

    @Transactional
    public ClosePositionResponse closePosition(ClosePositionRequest request) {

        Wallet wallet = walletRepository.findByUserIdForUpdate(request.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.HAS_NOT_WALLET));

        Position position = positionRepository.findByIdForUpdate(request.getPositionId())
                .orElseThrow(() -> new AppException(ErrorCode.POSITION_NOT_FOUND));

        if (!position.getWalletId().equals(wallet.getId())) {
            throw new AppException(ErrorCode.POSITION_NOT_BELONG_TO_WALLET);
        }
        if (position.getStatus() != PositionStatus.OPEN) {
            throw new AppException(ErrorCode.POSITION_ALREADY_CLOSED);
        }

        if (request.getClosedQuantity().compareTo(position.getQuantity()) > 0) {
            throw new AppException(ErrorCode.CLOSE_QUANTITY_EXCEEDS_POSITION);
        }

        BigDecimal closePrice = request.getCurrentPrice();
        boolean fullyClosed = request.getClosedQuantity().compareTo(position.getQuantity()) == 0;

        BigDecimal realizedPnl = calculateRealizedPnl(position, closePrice, request.getClosedQuantity());

        BigDecimal closeRatio = request.getClosedQuantity().divide(position.getQuantity(), 8, RoundingMode.HALF_UP);
        BigDecimal releasedMargin = position.getMargin()
                .multiply(closeRatio).setScale(8, RoundingMode.HALF_UP);

        BigDecimal amountToReturn = releasedMargin.add(realizedPnl);
        wallet.unlockMargin(amountToReturn,realizedPnl);
        walletRepository.save(wallet);

        if (fullyClosed) {
            position.setStatus(PositionStatus.CLOSED);
            position.setUpdatedAt(LocalDateTime.now());
            position.setQuantity(BigDecimal.ZERO);
            position.setMargin(BigDecimal.ZERO);
        } else {
            position.setQuantity(position.getQuantity().subtract(request.getClosedQuantity()));
            position.setMargin(position.getMargin().subtract(releasedMargin));
        }
        positionRepository.save(position);


        return ClosePositionResponse.builder()
                .success(true)
                .message("Close position successfully")
                .executedPrice(closePrice)
                .closedQuantity(request.getClosedQuantity())
                .realizedPnl(realizedPnl)
                .position(ClosePositionResponse.PositionData.builder()
                        .positionId(position.getId())
                        .symbol(position.getSymbol())
                        .side(position.getSide().name())
                        .quantity(position.getQuantity())
                        .averageEntryPrice(position.getAverageEntryPrice())
                        .leverage(position.getLeverage())
                        .margin(position.getMargin())
                        .liquidationPrice(fullyClosed ? null : position.getLiquidationPrice())
                        .status(position.getStatus().name())
                        .build())
                .wallet(ClosePositionResponse.WalletData.builder()
                        .balance(wallet.getBalance())
                        .lockBalance(wallet.getLockBalance())
                        .build())
                .build();
    }

    private BigDecimal calculateRealizedPnl(Position position, BigDecimal closePrice, BigDecimal closeQuantity) {
        BigDecimal priceDiff = position.getSide() == PositionSide.LONG
                ? closePrice.subtract(position.getAverageEntryPrice())
                : position.getAverageEntryPrice().subtract(closePrice);
        return priceDiff.multiply(closeQuantity).setScale(8, RoundingMode.HALF_UP);
    }
}
