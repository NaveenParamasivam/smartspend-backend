package com.smartspend.service;

import com.smartspend.dto.DTOs.*;
import com.smartspend.entity.Budget;
import com.smartspend.entity.Expense;
import com.smartspend.entity.Notification;
import com.smartspend.entity.User;
import com.smartspend.exception.AppException;
import com.smartspend.repository.BudgetRepository;
import com.smartspend.repository.ExpenseRepository;
import com.smartspend.repository.NotificationRepository;
import com.smartspend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SimpMessagingTemplate messagingTemplate;

    public ApiResponse<BudgetResponse> createOrUpdateBudget(String email, BudgetRequest request) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new AppException("User not found", 404));

        Optional<Budget> existing = budgetRepository.findByUserIdAndCategoryAndMonthAndYear(
            user.getId(), request.getCategory(), request.getMonth(), request.getYear());

        Budget budget;
        if (existing.isPresent()) {
            budget = existing.get();
            budget.setLimitAmount(request.getLimitAmount());
            budget.setAlertSent75(false);
            budget.setAlertSent100(false);
        } else {
            budget = Budget.builder()
                .user(user)
                .category(request.getCategory())
                .limitAmount(request.getLimitAmount())
                .month(request.getMonth())
                .year(request.getYear())
                .build();
        }

        budget = budgetRepository.save(budget);
        return ApiResponse.success("Budget saved successfully", mapToResponse(budget, user.getId()));
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<BudgetResponse>> getBudgets(String email, int month, int year) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new AppException("User not found", 404));

        List<BudgetResponse> budgets = budgetRepository
            .findByUserIdAndMonthAndYear(user.getId(), month, year)
            .stream()
            .map(b -> mapToResponse(b, user.getId()))
            .collect(Collectors.toList());

        return ApiResponse.success(budgets);
    }

    public ApiResponse<String> deleteBudget(String email, Long id) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new AppException("User not found", 404));
        Budget budget = budgetRepository.findByIdAndUserId(id, user.getId())
            .orElseThrow(() -> new AppException("Budget not found", 404));
        budgetRepository.delete(budget);
        return ApiResponse.success("Budget deleted successfully", null);
    }

    public void checkBudgetAlerts(User user, Expense.Category category, int month, int year) {
        budgetRepository.findByUserIdAndCategoryAndMonthAndYear(user.getId(), category, month, year)
            .ifPresent(budget -> {
                BigDecimal spent = expenseRepository.sumExpenseByUserIdAndCategoryAndMonthAndYear(
                    user.getId(), category, month, year);
                double percentage = spent.divide(budget.getLimitAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();

                String monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + year;

                if (percentage >= 100 && !budget.isAlertSent100()) {
                    budget.setAlertSent100(true);
                    budgetRepository.save(budget);
                    sendBudgetAlert(user, category.name(), percentage, monthName,
                        Notification.NotificationType.BUDGET_EXCEEDED);
                } else if (percentage >= 75 && !budget.isAlertSent75()) {
                    budget.setAlertSent75(true);
                    budgetRepository.save(budget);
                    sendBudgetAlert(user, category.name(), percentage, monthName,
                        Notification.NotificationType.BUDGET_WARNING);
                }
            });
    }

    private void sendBudgetAlert(User user, String category, double percentage,
                                  String month, Notification.NotificationType type) {
        String title = percentage >= 100 ? "Budget Exceeded!" : "Budget Warning";
        String message = String.format("Your %s budget for %s is at %.0f%%", category, month, percentage);

        Notification notification = Notification.builder()
            .user(user)
            .title(title)
            .message(message)
            .type(type)
            .build();
        notificationRepository.save(notification);

        // WebSocket push
        try {
            NotificationResponse wsPayload = NotificationResponse.builder()
                .title(title).message(message).type(type).read(false).build();
            messagingTemplate.convertAndSendToUser(user.getEmail(), "/queue/notifications", wsPayload);
        } catch (Exception e) {
            log.warn("WebSocket push failed: {}", e.getMessage());
        }

        // Email alert
        emailService.sendBudgetAlertEmail(user.getEmail(), user.getFirstName(), category, percentage, month);
    }

    public BudgetResponse mapToResponse(Budget budget, Long userId) {
        BigDecimal spent = expenseRepository.sumExpenseByUserIdAndCategoryAndMonthAndYear(
            userId, budget.getCategory(), budget.getMonth(), budget.getYear());
        BigDecimal remaining = budget.getLimitAmount().subtract(spent).max(BigDecimal.ZERO);
        double pct = spent.divide(budget.getLimitAmount(), 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100)).doubleValue();

        return BudgetResponse.builder()
            .id(budget.getId())
            .category(budget.getCategory())
            .limitAmount(budget.getLimitAmount())
            .spentAmount(spent)
            .remainingAmount(remaining)
            .percentageUsed(Math.min(pct, 100.0))
            .month(budget.getMonth())
            .year(budget.getYear())
            .createdAt(budget.getCreatedAt())
            .build();
    }
}
