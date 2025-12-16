package com.aaz.controller;

import com.aaz.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/trips/{tripId}/rates")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getRate(
            @PathVariable Long tripId,
            @RequestParam String from,
            @RequestParam String to) {

        BigDecimal rate = exchangeRateService.getRate(tripId, from, to);

        Map<String, Object> response = new HashMap<>();
        if (rate == null) {
            response.put("rate", null);
            response.put("message", "尚無匯率紀錄");
        } else {
            response.put("rate", rate);
            response.put("from", from);
            response.put("to", to);
        }

        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> updateRate(
            @PathVariable Long tripId,
            @RequestBody Map<String, Object> request) {

        String from = (String) request.get("from");
        String to = (String) request.get("to");
        BigDecimal rate = new BigDecimal(request.get("rate").toString());

        exchangeRateService.saveRate(tripId, from, to, rate);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "匯率已更新");

        return ResponseEntity.ok(response);
    }
}
