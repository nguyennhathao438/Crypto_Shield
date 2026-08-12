package com.crypto_shield.wallet_service.service;

import com.crypto_shield.wallet_service.component.PositionTriggerCommandProducer;
import com.crypto_shield.wallet_service.dto.PositionTriggerCommand;
import com.crypto_shield.wallet_service.dto.request.OpenPositionRequest;
import com.crypto_shield.wallet_service.dto.response.OpenPositionResponse;
import com.crypto_shield.wallet_service.entity.Position;
import com.crypto_shield.wallet_service.entity.Wallet;
import com.crypto_shield.wallet_service.enums.ErrorCode;
import com.crypto_shield.wallet_service.enums.PositionSide;
import com.crypto_shield.wallet_service.enums.PositionStatus;
import com.crypto_shield.wallet_service.enums.TriggerAction;
import com.crypto_shield.wallet_service.exception.AppException;
import com.crypto_shield.wallet_service.repository.PositionRepository;
import com.crypto_shield.wallet_service.repository.WalletRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class WalletPositionService {
    private final WalletRepository walletRepository;
    private final PositionRepository positionRepository;
    private final PositionTriggerCommandProducer triggerCommandProducer;
    private static final RoundingMode RM = RoundingMode.HALF_UP;
    private static final int SCALE = 8;

    @Transactional
    public OpenPositionResponse openPosition(OpenPositionRequest req) {

        // 1. Validate
        if (req.getQuantity() == null || req.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.INVALID_QUANTITY);
        }
        if (req.getLeverage() == null || req.getLeverage() <= 0) {
            throw new AppException(ErrorCode.INVALID_LEVERAGE);
        }

        //Notional = Price × Quantity
        //Margin = Notional / Leverage
        BigDecimal notional = req.getPrice().multiply(req.getQuantity());
        BigDecimal expectedMargin = notional.divide(BigDecimal.valueOf(req.getLeverage()), SCALE, RM);
        if (req.getMargin().subtract(expectedMargin).abs().compareTo(BigDecimal.valueOf(0.01)) > 0) {
            throw new AppException(ErrorCode.MARGIN_MISMATCH);
        }

        // 2. Lock wallet row to avoid race condition
        Wallet wallet = walletRepository.findByUserIdForUpdate(req.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.HAS_NOT_WALLET));

        // 3. Check wallet & Handle wallet
        if (wallet.getBalance().compareTo(req.getMargin()) < 0) {
            throw new AppException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        wallet.lockMargin(req.getMargin());
        walletRepository.save(wallet);

        // 4. Search position OPEN with symbol + side, it has then merge, if not then create
        PositionSide side = mapSide(req.getSide());
        Position position = positionRepository
                .findOpenPositionForUpdate(wallet.getId(), req.getSymbol(), side, PositionStatus.OPEN)
                .map(existing -> mergeIntoExisting(existing, req))
                .orElseGet(() -> createNewPosition(req, side));

        position.calculateLiquidationPrice();
        Position saved = positionRepository.save(position);
        triggerCommandProducer.send(PositionTriggerCommand.builder()
                .action(TriggerAction.REGISTER)
                .positionId(position.getId())
                .symbol(position.getSymbol())
                .liquidationPrice(position.getLiquidationPrice())
                .side(position.getSide())
                .build());
        // 5. Response data for Order Service
        return OpenPositionResponse.builder()
                .success(true)
                .message("Position opened successfully")
                .position(toPositionData(saved))
                .wallet(toWalletData(wallet))
                .build();
    }
    /**
     * Real-world Scenario Example (BTC/USDT - Isolated Long):
     *
     * 1. EXISTING POSITION:
     *    - Long 1 BTC @ $60,000 | Leverage 20x
     *    - Notional Value = 1 * $60,000 = $60,000
     *    - Current Margin = $60,000 / 20 = $3,000
     *
     * 2. NEW INCOMING ORDER (req):
     *    - Scale-in: Long 1 BTC @ $64,000 | Leverage 50x
     *    - Order Notional Value = 1 * $64,000 = $64,000
     *    - Order Margin = $64,000 / 50 = $1,280
     *
     * 3. MERGED POSITION RESULT:
     *    - Total Quantity = 1 + 1 = 2 BTC
     *    - Average Entry Price = (1 * 60,000 + 1 * 64,000) / 2 = $62,000
     *    - Total Margin = $3,000 + $1,280 = $4,280
     *    - Updated Leverage = 50x
     *    - Total Notional Value = 2 * $62,000 = $124,000
     *
     *  RISK ENGINE NOTE:
     *    At 50x leverage, theoretical Required Initial Margin = $124,000 / 50 = $2,480.
     *    However, the actual locked margin is $4,280 (carrying over $1,800 excess margin from the 20x position).
     *    -> The position has a safer margin buffer than a fresh 50x order,
     *       which results in a wider Liquidation Price gap.
     */
    private Position mergeIntoExisting(Position existing, OpenPositionRequest req) {
        BigDecimal oldQty = existing.getQuantity();
        BigDecimal newQty = req.getQuantity();
        BigDecimal totalQty = oldQty.add(newQty);

        // Average entry price = (oldQty*oldPrice + newQty*newPrice) / totalQty
        BigDecimal weightedOld = existing.getAverageEntryPrice().multiply(oldQty);
        BigDecimal weightedNew = req.getPrice().multiply(newQty);
        BigDecimal newAvgPrice = weightedOld.add(weightedNew)
                .divide(totalQty, SCALE, RM);

        existing.setQuantity(totalQty);
        existing.setAverageEntryPrice(newAvgPrice);
        existing.setMargin(existing.getMargin().add(req.getMargin()));
        existing.setLeverage(req.getLeverage());
        return existing;
    }
    private Position createNewPosition(OpenPositionRequest req, PositionSide side) {
        Wallet wallet = walletRepository.findByUserId(req.getUserId()).orElseThrow(()->new AppException(ErrorCode.HAS_NOT_WALLET));
        return Position.builder()
                .walletId(wallet.getId())
                .symbol(req.getSymbol())
                .side(side)
                .quantity(req.getQuantity())
                .averageEntryPrice(req.getPrice())
                .leverage(req.getLeverage())
                .margin(req.getMargin())
                .status(PositionStatus.OPEN)
                .build();
    }
    private PositionSide mapSide(String rawSide) {
        return switch (rawSide.toUpperCase()) {
            case "BUY" -> PositionSide.LONG;
            case "SELL" -> PositionSide.SHORT;
            default -> throw new AppException(ErrorCode.INVALID_SIDE);
        };
    }

    private OpenPositionResponse.PositionData toPositionData(Position p) {
        return OpenPositionResponse.PositionData.builder()
                .positionId(p.getId())
                .symbol(p.getSymbol())
                .side(p.getSide().name())
                .quantity(p.getQuantity())
                .averageEntryPrice(p.getAverageEntryPrice())
                .leverage(p.getLeverage())
                .margin(p.getMargin())
                .liquidationPrice(p.getLiquidationPrice())
                .status(p.getStatus().name())
                .build();
    }

    private OpenPositionResponse.WalletData toWalletData(Wallet w) {
        return OpenPositionResponse.WalletData.builder()
                .balance(w.getBalance())
                .lockBalance(w.getLockBalance())
                .build();
    }
}
