package com.crypto_shield.wallet_service.controller;

import com.crypto_shield.wallet_service.dto.OpenPositionRequest;
import com.crypto_shield.wallet_service.dto.OpenPositionResponse;
import com.crypto_shield.wallet_service.service.WalletPositionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/position")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PositionController {
   WalletPositionService walletPositionService;
    @PostMapping("/open")
    public ResponseEntity<OpenPositionResponse> openPosition(@RequestBody OpenPositionRequest req) {
        OpenPositionResponse res = walletPositionService.openPosition(req);
        return res.isSuccess() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }
}
