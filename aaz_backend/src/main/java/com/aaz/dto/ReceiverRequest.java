package com.aaz.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ReceiverRequest {
    private Long memberId;
    private BigDecimal amount;
}