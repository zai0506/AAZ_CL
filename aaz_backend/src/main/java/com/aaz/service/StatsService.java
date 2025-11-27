package com.aaz.service;

import com.aaz.dto.StatsReport;
import com.aaz.entity.Expense;
import com.aaz.entity.Income;
import com.aaz.repository.ExpenseRepository;
import com.aaz.repository.IncomeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;

    public StatsReport getExpenseStats(Long tripId) {
        List<Expense> expenses = expenseRepository.findByTripGroupId(tripId);

        // 計算總金額（使用換算後的金額）
        BigDecimal total = expenses.stream()
                .map(Expense::getConvertedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 按類別統計
        Map<String, BigDecimal> categoryAmounts = new HashMap<>();
        for (Expense expense : expenses) {
            String category = expense.getCategory();
            BigDecimal amount = expense.getConvertedAmount();
            categoryAmounts.put(category,
                    categoryAmounts.getOrDefault(category, BigDecimal.ZERO).add(amount));
        }

        // 計算百分比
        List<StatsReport.CategoryStat> categoryStats = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : categoryAmounts.entrySet()) {
            BigDecimal percentage = BigDecimal.ZERO;
            if (total.compareTo(BigDecimal.ZERO) > 0) {
                percentage = entry.getValue()
                        .divide(total, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
            }

            categoryStats.add(new StatsReport.CategoryStat(
                    entry.getKey(),
                    entry.getValue(),
                    percentage
            ));
        }

        // 按金額排序
        categoryStats.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));

        StatsReport report = new StatsReport();
        report.setType("expense");
        report.setTotal(total);
        report.setCategoryStats(categoryStats);
        return report;
    }

    public StatsReport getIncomeStats(Long tripId) {
        List<Income> incomes = incomeRepository.findByTripGroupId(tripId);

        // 計算總金額（使用換算後的金額）
        BigDecimal total = incomes.stream()
                .map(Income::getConvertedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 按類別統計
        Map<String, BigDecimal> categoryAmounts = new HashMap<>();
        for (Income income : incomes) {
            String category = income.getCategory();
            BigDecimal amount = income.getConvertedAmount();
            categoryAmounts.put(category,
                    categoryAmounts.getOrDefault(category, BigDecimal.ZERO).add(amount));
        }

        // 計算百分比
        List<StatsReport.CategoryStat> categoryStats = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : categoryAmounts.entrySet()) {
            BigDecimal percentage = BigDecimal.ZERO;
            if (total.compareTo(BigDecimal.ZERO) > 0) {
                percentage = entry.getValue()
                        .divide(total, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
            }

            categoryStats.add(new StatsReport.CategoryStat(
                    entry.getKey(),
                    entry.getValue(),
                    percentage
            ));
        }

        // 按金額排序
        categoryStats.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));

        StatsReport report = new StatsReport();
        report.setType("income");
        report.setTotal(total);
        report.setCategoryStats(categoryStats);
        return report;
    }
}