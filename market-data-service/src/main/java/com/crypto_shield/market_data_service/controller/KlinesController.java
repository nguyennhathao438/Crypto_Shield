package com.crypto_shield.market_data_service.controller;

import com.crypto_shield.market_data_service.dto.Candle;
import com.crypto_shield.market_data_service.dto.KlineRequest;
import com.crypto_shield.market_data_service.service.KlinesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/klines")
public class KlinesController {

    private final KlinesService klinesService;

    @GetMapping()
    public ResponseEntity<Map<String, Object>> getKlines(
            @RequestParam("symbol") String symbol,
            @RequestParam("interval") String interval,
            @RequestParam(value = "startTime",required = false) Long startTime,
            @RequestParam(value = "endTime",required = false) Long endTime,
            @RequestParam(value = "limit",required = false) Integer limit
    ) {
        KlineRequest request = new KlineRequest();
        request.setSymbol(symbol);
        request.setInterval(interval);
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        request.setLimit(limit);

        List<Candle> candles = klinesService.getKlines(request);

        Map<String, Object> response = new HashMap<>();
        response.put("symbol", symbol.toUpperCase());
        response.put("interval", interval);
        response.put("count", candles.size());
        response.put("data", candles);

        return ResponseEntity.ok(response);
    }
}