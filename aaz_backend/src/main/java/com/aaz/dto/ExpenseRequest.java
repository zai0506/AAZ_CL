package com.aaz.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class ExpenseRequest {
    private String title;
    private String category;
    private BigDecimal amount;
    private String currency;
    private LocalDate expenseDate;
    private String notes;
    private List<PaymentRequest> payments;
    private List<SplitRequest> splits;
    private BigDecimal exchangeRate;
}