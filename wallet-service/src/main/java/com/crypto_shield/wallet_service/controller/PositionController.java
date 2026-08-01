package com.crypto_shield.wallet_service.controller;

import com.crypto_shield.wallet_service.dto.OpenPositionRequest;
import com.crypto_shield.wallet_service.dto.OpenPositionResponse;
import com.crypto_shield.wallet_service.dto.PositionResponse;
import com.crypto_shield.wallet_service.service.PositionService;
import com.crypto_shield.wallet_service.service.WalletPositionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/position")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PositionController {
   WalletPositionService walletPositionService;
   PositionService positionService;
    @PostMapping("/open")
    public ResponseEntity<OpenPositionResponse> openPosition(@RequestBody OpenPositionRequest req) {
        OpenPositionResponse res = walletPositionService.openPosition(req);
        return res.isSuccess() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<List<PositionResponse>> streamPositions(@RequestHeader("X-User-Id") UUID userId) {
        return positionService.streamPositions(userId);
    }
}
