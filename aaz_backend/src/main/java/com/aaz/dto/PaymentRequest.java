package com.aaz.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentRequest {
    private Long memberId;
    private BigDecimal amount;
}