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
