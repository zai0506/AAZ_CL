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

    @PutMapping("/expenses/{transactionId}")
    public ResponseEntity<TransactionResponse> updateExpense(
            @PathVariable Long tripId,
            @PathVariable Long transactionId,
            @RequestBody ExpenseRequest request) {
        return ResponseEntity.ok(transactionService.updateExpense(tripId, transactionId, request));
    }

    @PutMapping("/incomes/{transactionId}")
    public ResponseEntity<TransactionResponse> updateIncome(
            @PathVariable Long tripId,
            @PathVariable Long transactionId,
            @RequestBody IncomeRequest request) {
        return ResponseEntity.ok(transactionService.updateIncome(tripId, transactionId, request));
    }

    @PutMapping("/transfers/{transactionId}")
    public ResponseEntity<TransactionResponse> updateTransfer(
            @PathVariable Long tripId,
            @PathVariable Long transactionId,
            @RequestBody TransferRequest request) {
        return ResponseEntity.ok(transactionService.updateTransfer(tripId, transactionId, request));
    }

    @DeleteMapping("/expenses/{transactionId}")
    public ResponseEntity<Void> deleteExpense(
            @PathVariable Long tripId,
            @PathVariable Long transactionId) {
        transactionService.deleteExpense(tripId, transactionId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/incomes/{transactionId}")
    public ResponseEntity<Void> deleteIncome(
            @PathVariable Long tripId,
            @PathVariable Long transactionId) {
        transactionService.deleteIncome(tripId, transactionId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/transfers/{transactionId}")
    public ResponseEntity<Void> deleteTransfer(
            @PathVariable Long tripId,
            @PathVariable Long transactionId) {
        transactionService.deleteTransfer(tripId, transactionId);
        return ResponseEntity.ok().build();
    }
}