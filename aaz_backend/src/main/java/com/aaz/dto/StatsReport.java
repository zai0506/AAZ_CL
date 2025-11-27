package com.aaz.dto;

import lombok.AllArgsConstructor; // ← 新增這行
import lombok.Data;
import lombok.NoArgsConstructor; // ← 新增這行
import java.math.BigDecimal;
import java.util.List;

@Data
public class StatsReport {
    private String type;
    private BigDecimal total;
    private List<CategoryStat> categoryStats;

    @Data
    @NoArgsConstructor
    public static class CategoryStat {
        private String category;
        private BigDecimal amount;
        private BigDecimal percentage;
        private List<TransactionResponse> transactions;

        // 自定義建構子：只接受 3 個參數
        public CategoryStat(String category, BigDecimal amount, BigDecimal percentage) {
            this.category = category;
            this.amount = amount;
            this.percentage = percentage;
        }
    }
}