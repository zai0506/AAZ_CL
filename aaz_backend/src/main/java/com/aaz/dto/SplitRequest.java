package com.aaz.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SplitRequest {
    private Long memberId;
    private BigDecimal amount;
    private String splitType;
}