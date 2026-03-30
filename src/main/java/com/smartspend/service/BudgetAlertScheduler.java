package com.smartspend.service;

import com.smartspend.entity.Budget;
import com.smartspend.entity.Expense;
import com.smartspend.repository.BudgetRepository;
import com.smartspend.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Scheduled job that runs nightly to check all budgets and send alerts.
 * This supplements the real-time checks in BudgetService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetAlertScheduler {

    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final BudgetService budgetService;

    // Runs at midnight every day
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void checkBudgetAlerts() {
        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();
        int year  = now.getYear();
        String monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + year;

        log.info("Running scheduled budget alert check for {}", monthName);

        // Check budgets nearing 75%
        List<Budget> budgets75 = budgetRepository.findByMonthAndYearAndAlertSent75False(month, year);
        for (Budget b : budgets75) {
            BigDecimal spent = expenseRepository.sumExpenseByUserIdAndCategoryAndMonthAndYear(
                b.getUser().getId(), b.getCategory(), month, year);
            double pct = spent.divide(b.getLimitAmount(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).doubleValue();
            if (pct >= 75) {
                budgetService.checkBudgetAlerts(b.getUser(), b.getCategory(), month, year);
            }
        }

        // Check budgets exceeding 100%
        List<Budget> budgets100 = budgetRepository.findByMonthAndYearAndAlertSent100False(month, year);
        for (Budget b : budgets100) {
            BigDecimal spent = expenseRepository.sumExpenseByUserIdAndCategoryAndMonthAndYear(
                b.getUser().getId(), b.getCategory(), month, year);
            double pct = spent.divide(b.getLimitAmount(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).doubleValue();
            if (pct >= 100) {
                budgetService.checkBudgetAlerts(b.getUser(), b.getCategory(), month, year);
            }
        }

        log.info("Budget alert check complete. Checked {} budgets at 75%, {} at 100%",
            budgets75.size(), budgets100.size());
    }
}
