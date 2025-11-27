package com.aaz.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class TransactionResponse {
    private String transactionId;
    private String type;
    private String title;
    private String category;
    private BigDecimal amount;
    private String currency;
    private BigDecimal convertedAmount;
    private LocalDate transactionDate;
    private String notes;
    private List<PaymentResponse> payments;
    private List<SplitResponse> splits;
}