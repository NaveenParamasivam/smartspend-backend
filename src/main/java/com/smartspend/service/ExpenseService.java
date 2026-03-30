package com.smartspend.service;

import com.smartspend.dto.DTOs.*;
import com.smartspend.entity.Expense;
import com.smartspend.entity.User;
import com.smartspend.exception.AppException;
import com.smartspend.repository.BudgetRepository;
import com.smartspend.repository.ExpenseRepository;
import com.smartspend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final BudgetService budgetService;

    public ApiResponse<ExpenseResponse> createExpense(String email, ExpenseRequest request) {
        User user = getUserByEmail(email);
        Expense expense = Expense.builder()
            .title(request.getTitle())
            .amount(request.getAmount())
            .category(request.getCategory())
            .type(request.getType())
            .date(request.getDate())
            .description(request.getDescription())
            .user(user)
            .build();

        expense = expenseRepository.save(expense);

        // Check budget alerts after saving expense
        if (request.getType() == Expense.TransactionType.EXPENSE) {
            budgetService.checkBudgetAlerts(user, request.getCategory(),
                expense.getDate().getMonthValue(), expense.getDate().getYear());
        }

        return ApiResponse.success("Expense created successfully", mapToResponse(expense));
    }

    @Transactional(readOnly = true)
    public ApiResponse<PageResponse<ExpenseResponse>> getExpenses(String email, ExpenseFilterRequest filter) {
        User user = getUserByEmail(email);

        String sortBy = filter.getSortBy() != null ? filter.getSortBy() : "date";
        Sort.Direction dir = "asc".equalsIgnoreCase(filter.getSortDir())
            ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageRequest = PageRequest.of(filter.getPage(), filter.getSize(), Sort.by(dir, sortBy));

        Page<Expense> page = expenseRepository.findWithFilters(
            user.getId(),
            filter.getStartDate(),
            filter.getEndDate(),
            filter.getCategory(),
            filter.getType(),
            filter.getMinAmount(),
            filter.getMaxAmount(),
            pageRequest
        );

        PageResponse<ExpenseResponse> response = PageResponse.<ExpenseResponse>builder()
            .content(page.getContent().stream().map(this::mapToResponse).collect(Collectors.toList()))
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .last(page.isLast())
            .build();

        return ApiResponse.success(response);
    }

    @Transactional(readOnly = true)
    public ApiResponse<ExpenseResponse> getExpenseById(String email, Long id) {
        User user = getUserByEmail(email);
        Expense expense = expenseRepository.findByIdAndUserId(id, user.getId())
            .orElseThrow(() -> new AppException("Expense not found", 404));
        return ApiResponse.success(mapToResponse(expense));
    }

    public ApiResponse<ExpenseResponse> updateExpense(String email, Long id, ExpenseRequest request) {
        User user = getUserByEmail(email);
        Expense expense = expenseRepository.findByIdAndUserId(id, user.getId())
            .orElseThrow(() -> new AppException("Expense not found", 404));

        expense.setTitle(request.getTitle());
        expense.setAmount(request.getAmount());
        expense.setCategory(request.getCategory());
        expense.setType(request.getType());
        expense.setDate(request.getDate());
        expense.setDescription(request.getDescription());
        expense = expenseRepository.save(expense);

        if (request.getType() == Expense.TransactionType.EXPENSE) {
            budgetService.checkBudgetAlerts(user, request.getCategory(),
                request.getDate().getMonthValue(), request.getDate().getYear());
        }

        return ApiResponse.success("Expense updated successfully", mapToResponse(expense));
    }

    public ApiResponse<String> deleteExpense(String email, Long id) {
        User user = getUserByEmail(email);
        Expense expense = expenseRepository.findByIdAndUserId(id, user.getId())
            .orElseThrow(() -> new AppException("Expense not found", 404));
        expenseRepository.delete(expense);
        return ApiResponse.success("Expense deleted successfully", null);
    }

    @Transactional(readOnly = true)
    public List<Expense> getExpensesForReport(String email, LocalDate startDate, LocalDate endDate) {
        User user = getUserByEmail(email);
        return expenseRepository.findByUserIdAndDateBetween(user.getId(), startDate, endDate);
    }

    public ExpenseResponse mapToResponse(Expense expense) {
        return ExpenseResponse.builder()
            .id(expense.getId())
            .title(expense.getTitle())
            .amount(expense.getAmount())
            .category(expense.getCategory())
            .type(expense.getType())
            .date(expense.getDate())
            .description(expense.getDescription())
            .createdAt(expense.getCreatedAt())
            .updatedAt(expense.getUpdatedAt())
            .build();
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new AppException("User not found", 404));
    }
}
