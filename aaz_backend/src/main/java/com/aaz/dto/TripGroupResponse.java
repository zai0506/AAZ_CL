package com.aaz.dto;

import lombok.Data;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

@Data
public class TripGroupResponse {
    private Long id;
    private String tripCode;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private String baseCurrency;
    private String announcement;
    private List<MemberResponse> members;
    private BigDecimal totalExpense;
    private BigDecimal totalIncome;
}