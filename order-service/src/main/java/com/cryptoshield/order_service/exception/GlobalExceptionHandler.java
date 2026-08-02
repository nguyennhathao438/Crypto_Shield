package com.cryptoshield.order_service.exception;

import com.cryptoshield.order_service.dto.response.ApiResponse;
import com.cryptoshield.order_service.enums.ErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponse<String>> handlingAppException(AppException  exception){
        ErrorCode errorCode =exception.getErrorCode();
        ApiResponse<String> apiResponse = new ApiResponse<>();

        String message = (exception.getMessage() != null)
                ? exception.getMessage()
                : errorCode.getMessage();
        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(message);
        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }
    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResponse<String>> handlingException(Exception  exception){
        exception.printStackTrace();
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setCode(ErrorCode.UNCATEGORED_EXCEPTION.getCode());
        apiResponse.setMessage(ErrorCode.UNCATEGORED_EXCEPTION.getMessage());
        return ResponseEntity.badRequest().body(apiResponse);
    }
}
