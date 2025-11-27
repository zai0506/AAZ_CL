package com.aaz.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TransferRequest {
    private Long fromMemberId;
    private Long toMemberId;
    private BigDecimal amount;
    private String currency;
    private LocalDate transferDate;
    private String notes;
    private BigDecimal exchangeRate;
}