package com.aaz.controller;

import com.aaz.dto.*;
import com.aaz.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips/{tripId}")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionResponse>> getAllTransactions(
            @PathVariable Long tripId) {
        return ResponseEntity.ok(transactionService.getAllTransactions(tripId));
    }

    @PostMapping("/expenses")
    public ResponseEntity<TransactionResponse> createExpense(
            @PathVariable Long tripId,
            @RequestBody ExpenseRequest request) {
        return ResponseEntity.ok(transactionService.createExpense(tripId, request));
    }

    @PostMapping("/incomes")
    public ResponseEntity<TransactionResponse> createIncome(
            @PathVariable Long tripId,
            @RequestBody IncomeRequest request) {
        return ResponseEntity.ok(transactionService.createIncome(tripId, request));
    }

    @PostMapping("/transfers")
    public ResponseEntity<TransactionResponse> createTransfer(
            @PathVariable Long tripId,
            @RequestBody TransferRequest request) {
        return ResponseEntity.ok(transactionService.createTransfer(tripId, request));
    }
}