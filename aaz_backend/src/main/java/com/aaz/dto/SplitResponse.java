package com.aaz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SplitResponse {
    private Long memberId;
    private String memberName;
    private BigDecimal amount;
    private String splitType;
}