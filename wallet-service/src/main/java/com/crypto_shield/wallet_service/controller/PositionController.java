package com.crypto_shield.wallet_service.controller;

import com.crypto_shield.wallet_service.dto.request.ClosePositionRequest;
import com.crypto_shield.wallet_service.dto.request.OpenPositionRequest;
import com.crypto_shield.wallet_service.dto.response.ClosePositionResponse;
import com.crypto_shield.wallet_service.dto.response.OpenPositionResponse;
import com.crypto_shield.wallet_service.dto.response.PositionResponse;
import com.crypto_shield.wallet_service.service.ClosePositionService;
import com.crypto_shield.wallet_service.service.PositionService;
import com.crypto_shield.wallet_service.service.WalletPositionService;
import jakarta.validation.Valid;
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
   ClosePositionService closePositionService;
    @PostMapping("/open")
    public ResponseEntity<OpenPositionResponse> openPosition(@RequestBody @Valid OpenPositionRequest req) {
        OpenPositionResponse res = walletPositionService.openPosition(req);
        return res.isSuccess() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }
    @PostMapping("/close")
    public ResponseEntity<ClosePositionResponse> closenPosition(@RequestBody @Valid ClosePositionRequest req) {
        ClosePositionResponse res = closePositionService.closePosition(req);
        return res.isSuccess() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<List<PositionResponse>> streamPositions(@RequestHeader("X-User-Id") UUID userId) {
        return positionService.streamPositions(userId);
    }
}
