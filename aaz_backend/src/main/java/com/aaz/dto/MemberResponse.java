package com.aaz.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class MemberResponse {
    private Long id;
    private String displayName;
    private Boolean isCreator;
    private BigDecimal balance;
}