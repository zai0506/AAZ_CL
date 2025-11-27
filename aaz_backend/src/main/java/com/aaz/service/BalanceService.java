package com.aaz.service;

import com.aaz.dto.BalanceReport;
import com.aaz.entity.*;
import com.aaz.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BalanceService {

    private final MemberRepository memberRepository;
    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final TransferRepository transferRepository;

    public BalanceReport calculateBalances(Long tripId) {
        List<Member> members = memberRepository.findByTripGroupId(tripId);
        Map<Long, BigDecimal> balances = new HashMap<>();

        // 初始化所有成員餘額為 0
        for (Member member : members) {
            balances.put(member.getId(), BigDecimal.ZERO);
        }

        // 處理支出
        List<Expense> expenses = expenseRepository.findByTripGroupId(tripId);
        for (Expense expense : expenses) {
            // 付款人增加餘額（他們先墊錢）
            for (ExpensePayment payment : expense.getPayments()) {
                Long memberId = payment.getMember().getId();
                balances.put(memberId, balances.get(memberId).add(payment.getAmount()));
            }

            // 分攤者減少餘額（他們欠錢）
            for (ExpenseSplit split : expense.getSplits()) {
                Long memberId = split.getMember().getId();
                balances.put(memberId, balances.get(memberId).subtract(split.getAmount()));
            }
        }

        // 處理收入
        List<Income> incomes = incomeRepository.findByTripGroupId(tripId);
        for (Income income : incomes) {
            // 收款人減少餘額（他們先收到錢）
            for (IncomeReceiver receiver : income.getReceivers()) {
                Long memberId = receiver.getMember().getId();
                balances.put(memberId, balances.get(memberId).subtract(receiver.getAmount()));
            }

            // 分攤者增加餘額（他們應得）
            for (IncomeSplit split : income.getSplits()) {
                Long memberId = split.getMember().getId();
                balances.put(memberId, balances.get(memberId).add(split.getAmount()));
            }
        }

        // 處理轉帳
        List<Transfer> transfers = transferRepository.findByTripGroupId(tripId);
        for (Transfer transfer : transfers) {
            // 轉出者減少餘額
            Long fromId = transfer.getFromMember().getId();
            balances.put(fromId, balances.get(fromId).subtract(transfer.getAmount()));

            // 轉入者增加餘額
            Long toId = transfer.getToMember().getId();
            balances.put(toId, balances.get(toId).add(transfer.getAmount()));
        }

        // 建立餘額報表
        List<BalanceReport.MemberBalance> memberBalances = new ArrayList<>();
        for (Member member : members) {
            BigDecimal balance = balances.get(member.getId());
            memberBalances.add(new BalanceReport.MemberBalance(
                    member.getId(),
                    member.getDisplayName(),
                    balance
            ));
        }

        // 計算最少轉帳方案
        List<BalanceReport.Debt> debts = minimizeTransactions(balances, members);

        BalanceReport report = new BalanceReport();
        report.setBalances(memberBalances);
        report.setDebts(debts);
        return report;
    }

    /**
     * 使用貪心算法計算最少轉帳次數
     */
    private List<BalanceReport.Debt> minimizeTransactions(
            Map<Long, BigDecimal> balances,
            List<Member> members) {

        List<BalanceReport.Debt> debts = new ArrayList<>();

        // 建立債權人和債務人的優先隊列
        PriorityQueue<MemberBalance> creditors = new PriorityQueue<>(
                (a, b) -> b.balance.compareTo(a.balance)
        );
        PriorityQueue<MemberBalance> debtors = new PriorityQueue<>(
                (a, b) -> a.balance.compareTo(b.balance)
        );

        // 分類債權人和債務人
        Map<Long, String> memberNames = members.stream()
                .collect(Collectors.toMap(Member::getId, Member::getDisplayName));

        for (Map.Entry<Long, BigDecimal> entry : balances.entrySet()) {
            BigDecimal balance = entry.getValue();
            if (balance.compareTo(BigDecimal.ZERO) > 0) {
                creditors.offer(new MemberBalance(entry.getKey(), balance));
            } else if (balance.compareTo(BigDecimal.ZERO) < 0) {
                debtors.offer(new MemberBalance(entry.getKey(), balance.abs()));
            }
        }

        // 貪心配對
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            MemberBalance creditor = creditors.poll();
            MemberBalance debtor = debtors.poll();

            BigDecimal amount = creditor.balance.min(debtor.balance);

            debts.add(new BalanceReport.Debt(
                    memberNames.get(debtor.memberId),
                    memberNames.get(creditor.memberId),
                    amount
            ));

            creditor.balance = creditor.balance.subtract(amount);
            debtor.balance = debtor.balance.subtract(amount);

            if (creditor.balance.compareTo(BigDecimal.ZERO) > 0) {
                creditors.offer(creditor);
            }
            if (debtor.balance.compareTo(BigDecimal.ZERO) > 0) {
                debtors.offer(debtor);
            }
        }

        return debts;
    }

    private static class MemberBalance {
        Long memberId;
        BigDecimal balance;

        MemberBalance(Long memberId, BigDecimal balance) {
            this.memberId = memberId;
            this.balance = balance;
        }
    }
}