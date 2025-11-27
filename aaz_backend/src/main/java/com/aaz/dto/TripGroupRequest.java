package com.aaz.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class TripGroupRequest {
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private String baseCurrency;
    private String announcement;
    private List<MemberRequest> members;
}