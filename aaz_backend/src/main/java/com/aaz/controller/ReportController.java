package com.aaz.controller;

import com.aaz.dto.*;
import com.aaz.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<StatsReport> getExpenseStats(@PathVariable Long tripId) {
        return ResponseEntity.ok(statsService.getExpenseStats(tripId));
    }
    
    @GetMapping("/stats/incomes")
    public ResponseEntity<StatsReport> getIncomeStats(@PathVariable Long tripId) {
        return ResponseEntity.ok(statsService.getIncomeStats(tripId));
    }
}