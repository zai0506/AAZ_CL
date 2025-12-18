package com.aaz.controller;

import com.aaz.dto.*;
import com.aaz.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/trips/{tripId}")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReportController {

    private final BalanceService balanceService;
    private final StatsService statsService;

    @GetMapping("/settlement")
    public ResponseEntity<BalanceReport> getSettlement(@PathVariable Long tripId) {
        return ResponseEntity.ok(balanceService.calculateBalances(tripId));
    }

    @GetMapping("/stats/expenses")
    public ResponseEntity<StatsReport> getExpenseStats(
            @PathVariable Long tripId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(statsService.getExpenseStats(tripId, startDate, endDate));
    }

    @GetMapping("/stats/incomes")
    public ResponseEntity<StatsReport> getIncomeStats(
            @PathVariable Long tripId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(statsService.getIncomeStats(tripId, startDate, endDate));
    }
}