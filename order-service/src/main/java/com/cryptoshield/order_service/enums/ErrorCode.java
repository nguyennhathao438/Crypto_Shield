package com.cryptoshield.order_service.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
@Getter
public enum ErrorCode {
    INSUFFICIENT_BALANCE(1001, "Insufficient balance", HttpStatus.CONFLICT),
    MARKET_DATA_UNAVAILABLE(1002,"Market data unavaiable",HttpStatus.BAD_REQUEST),
    MARKET_DATA_SERVICE_ERROR(1003,"Request to market data service failed",HttpStatus.BAD_REQUEST),
    WALLET_SERVICE_ERROR(1004,"Request to wallet service failed",HttpStatus.BAD_REQUEST),
    SLIPPAGE_EXCEEDED(1005,"Slippage exceeded",HttpStatus.BAD_REQUEST),

    POSITION_NOT_FOUND(1101, "Position not found", HttpStatus.NOT_FOUND),
    POSITION_NOT_OPEN(1102, "Position is not open", HttpStatus.CONFLICT),
    POSITION_ALREADY_CLOSED(1103, "Position already closed", HttpStatus.CONFLICT),
    POSITION_NOT_BELONG_TO_USER(1104, "Position does not belong to this user", HttpStatus.FORBIDDEN),

    ORDER_CONDITION_NOT_FOUND(1201, "Order condition not found", HttpStatus.NOT_FOUND),
    ORDER_CONDITION_NOT_PENDING(1202, "Order condition is not in pending status", HttpStatus.CONFLICT),
    ORDER_CONDITION_ALREADY_TRIGGERED(1203, "Order condition already triggered", HttpStatus.CONFLICT),
    INVALID_TRIGGER_PRICE(1204, "Trigger price is invalid for current market price and position side", HttpStatus.BAD_REQUEST),
    QUANTITY_EXCEEDS_AVAILABLE(1205, "Requested quantity exceeds available position quantity", HttpStatus.BAD_REQUEST),
    INVALID_ORDER_CONDITION_TYPE(1206, "Invalid order condition type", HttpStatus.BAD_REQUEST),
    POSITION_SIDE_NOT_VALID(1207, "Position side is not valid", HttpStatus.BAD_REQUEST),

    INVALID_LIMIT_PRICE(1208, "Limit price is invalid compared to current market price", HttpStatus.BAD_REQUEST),
    ORDER_NOT_FOUND(1209, "Order not found", HttpStatus.NOT_FOUND),
    ORDER_NOT_BELONG_TO_USER(1210, "Order does not belong to this user", HttpStatus.FORBIDDEN),
    ORDER_NOT_PENDING(1211, "Order is not in pending status", HttpStatus.CONFLICT),
    INVALID_ORDER_TYPE(1212, "Invalid order type for this operation", HttpStatus.BAD_REQUEST),
    INVALID_MARGIN(1213, "Margin does not match the order details", HttpStatus.BAD_REQUEST),

    WALLET_SERVICE_TIMEOUT(1301, "Wallet service did not respond in time", HttpStatus.GATEWAY_TIMEOUT),
    MARKET_DATA_SERVICE_TIMEOUT(1302, "Market data service did not respond in time", HttpStatus.GATEWAY_TIMEOUT),
    UNCATEGORED_EXCEPTION(9999, "Uncategorized exception", HttpStatus.INTERNAL_SERVER_ERROR);
    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

}
