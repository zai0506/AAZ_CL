package com.aaz.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class IncomeRequest {
    private String title;
    private String category;
    private BigDecimal amount;
    private String currency;
    private LocalDate incomeDate;
    private String notes;
    private List<ReceiverRequest> receivers;
    private List<SplitRequest> splits;
    private BigDecimal exchangeRate;
}