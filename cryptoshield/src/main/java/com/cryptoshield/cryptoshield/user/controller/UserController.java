package com.cryptoshield.cryptoshield.user.controller;

import com.cryptoshield.cryptoshield.user.dto.ApiResponse;
import com.cryptoshield.cryptoshield.user.dto.UserRequest;
import com.cryptoshield.cryptoshield.user.dto.UserResponse;
import com.cryptoshield.cryptoshield.user.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {
    UserService userService;
    @PostMapping("/login")
    ResponseEntity<ApiResponse<UserResponse>> authenticate(@RequestBody UserRequest request) {
        var result = userService.login(request);

        ResponseCookie cookie = ResponseCookie.from("refreshToken",
                        result.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofDays(7))
                .sameSite("Strict")
                .build();
        result.setRefreshToken(null);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.<UserResponse>builder()
                        .result(result)
                        .build());

    }
    @PostMapping("/logout")
    ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String authorization,
                                             @CookieValue(value = "refreshToken", required = false) String refreshToken) {
        String accessToken = authorization.substring(7);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();
        userService.logout(accessToken,refreshToken);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.<Void>builder()
                        .build());
    }
    @PostMapping("/register")
    ResponseEntity<ApiResponse<UserResponse>> createUser(@RequestBody UserRequest request) {
        UserResponse result = userService.registerUser(request);
        ResponseCookie cookie = ResponseCookie.from("refreshToken",
                        result.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofDays(7))
                .sameSite("Strict")
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.<UserResponse>builder()
                        .result(result)
                        .build());
    }
}
