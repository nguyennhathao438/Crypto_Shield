package com.crypto_shield.wallet_service.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
@Getter
public enum ErrorCode {
    HAS_HAVE_WALLET(1001, "User has have wallet", HttpStatus.CONFLICT),
    HAS_NOT_WALLET(1002,"User hasn't wallet", HttpStatus.NOT_FOUND),
    MARGIN_MISMATCH(1003,"Margin isn't match with recipe",HttpStatus.BAD_REQUEST),
    INVALID_QUANTITY(1004,"Quantity isn't valid",HttpStatus.BAD_REQUEST),
    INVALID_LEVERAGE(1005,"Leverage isn't valid",HttpStatus.BAD_REQUEST),
    INVALID_SIDE(1006,"Side isn't valid",HttpStatus.BAD_REQUEST),
    INSUFFICIENT_BALANCE(1007,"insufficient balance",HttpStatus.BAD_REQUEST),
    POSITION_NOT_FOUND(1008, "Position not found", HttpStatus.NOT_FOUND),
    POSITION_NOT_BELONG_TO_WALLET(1009, "Position doesn't belong to this wallet", HttpStatus.FORBIDDEN),
    POSITION_ALREADY_CLOSED(1010, "Position is already closed", HttpStatus.CONFLICT),
    CLOSE_QUANTITY_EXCEEDS_POSITION(1012, "Close quantity exceeds remaining position quantity", HttpStatus.BAD_REQUEST),
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
