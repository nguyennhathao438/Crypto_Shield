package com.crypto_shield.wallet_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
@Getter
public enum ErrorCode {
    HAS_HAVE_WALLET(1001, "User has have wallet", HttpStatus.CONFLICT),
    HAS_NOT_WALLET(1002,"User hasn't wallet", HttpStatus.NOT_FOUND),
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
