package com.smartspend.service;

import com.smartspend.dto.DTOs;
import com.smartspend.dto.DTOs.*;
import com.smartspend.entity.Expense;
import com.smartspend.entity.User;
import com.smartspend.exception.AppException;
import com.smartspend.repository.BudgetRepository;
import com.smartspend.repository.ExpenseRepository;
import com.smartspend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final BudgetService budgetService;
    private final ExpenseService expenseService;

    public ApiResponse<DashboardResponse> getDashboard(String email, int month, int year) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new AppException("User not found", 404));

        BigDecimal totalIncome = expenseRepository.sumByUserIdAndTypeAndMonthAndYear(
            user.getId(), Expense.TransactionType.INCOME, month, year);
        BigDecimal totalExpense = expenseRepository.sumByUserIdAndTypeAndMonthAndYear(
            user.getId(), Expense.TransactionType.EXPENSE, month, year);
        BigDecimal netBalance = totalIncome.subtract(totalExpense);
        long totalTransactions = expenseRepository.countByUserId(user.getId());

        // Category breakdown
        List<Object[]> categoryData = expenseRepository.sumExpenseByCategory(user.getId(), month, year);
        List<CategorySummary> categoryBreakdown = categoryData.stream()
            .map(row -> {
                BigDecimal amt = (BigDecimal) row[1];
                double pct = totalExpense.compareTo(BigDecimal.ZERO) == 0 ? 0 :
                    amt.divide(totalExpense, 4, RoundingMode.HALF_UP)
                       .multiply(BigDecimal.valueOf(100)).doubleValue();
                return CategorySummary.builder()
                    .category((Expense.Category) row[0])
                    .amount(amt)
                    .percentage(pct)
                    .build();
            }).collect(Collectors.toList());

        // Monthly trends for the year
        List<Object[]> incomeMonthly = expenseRepository.monthlyTotalsByType(
            user.getId(), Expense.TransactionType.INCOME, year);
        List<Object[]> expenseMonthly = expenseRepository.monthlyTotalsByType(
            user.getId(), Expense.TransactionType.EXPENSE, year);

        Map<Integer, BigDecimal> incomeMap = new HashMap<>();
        Map<Integer, BigDecimal> expenseMap = new HashMap<>();
        incomeMonthly.forEach(r -> incomeMap.put(((Number) r[0]).intValue(), (BigDecimal) r[1]));
        expenseMonthly.forEach(r -> expenseMap.put(((Number) r[0]).intValue(), (BigDecimal) r[1]));

        List<MonthlyTrend> trends = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            trends.add(MonthlyTrend.builder()
                .month(m)
                .monthName(Month.of(m).getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                .income(incomeMap.getOrDefault(m, BigDecimal.ZERO))
                .expense(expenseMap.getOrDefault(m, BigDecimal.ZERO))
                .build());
        }

        // Budget status
        List<BudgetResponse> budgets = budgetRepository
            .findByUserIdAndMonthAndYear(user.getId(), month, year)
            .stream()
            .map(b -> budgetService.mapToResponse(b, user.getId()))
            .collect(Collectors.toList());

        // Recent transactions (last 5)
        PageRequest recent = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "date"));
        List<ExpenseResponse> recentTx = expenseRepository.findByUserId(user.getId(), recent)
            .getContent().stream()
            .map(expenseService::mapToResponse)
            .collect(Collectors.toList());

        DashboardResponse dashboard = DashboardResponse.builder()
            .totalIncome(totalIncome)
            .totalExpense(totalExpense)
            .netBalance(netBalance)
            .totalTransactions(totalTransactions)
            .categoryBreakdown(categoryBreakdown)
            .monthlyTrends(trends)
            .budgetStatus(budgets)
            .recentTransactions(recentTx)
            .build();

        return ApiResponse.success(dashboard);
    }
}
