package com.aaz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
public class BalanceReport {
    private List<MemberBalance> balances;
    private List<Debt> debts;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberBalance {
        private Long memberId;
        private String memberName;
        private BigDecimal balance;
    }

    @Data
    @NoArgsConstructor
    public static class Debt {
        private String fromMemberName;
        private String toMemberName;
        private BigDecimal amount;
        private Long fromMemberId;
        private Long toMemberId;

        // 自定義建構子：只接受 3 個參數
        public Debt(String fromMemberName, String toMemberName, BigDecimal amount) {
            this.fromMemberName = fromMemberName;
            this.toMemberName = toMemberName;
            this.amount = amount;
        }
    }
}