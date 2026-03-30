package com.smartspend.controller;

import com.smartspend.dto.DTOs.*;
import com.smartspend.service.BudgetService;
import com.smartspend.service.DashboardService;
import com.smartspend.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

// ===================== Budget Controller =====================
@RestController
@RequestMapping("/budgets")
@RequiredArgsConstructor
class BudgetController {
    private final BudgetService budgetService;

    @PostMapping
    public ResponseEntity<ApiResponse<BudgetResponse>> create(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody BudgetRequest request) {
        return ResponseEntity.ok(budgetService.createOrUpdateBudget(user.getUsername(), request));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> get(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(defaultValue = "0") int month,
            @RequestParam(defaultValue = "0") int year) {
        int m = month == 0 ? LocalDate.now().getMonthValue() : month;
        int y = year == 0 ? LocalDate.now().getYear() : year;
        return ResponseEntity.ok(budgetService.getBudgets(user.getUsername(), m, y));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id) {
        return ResponseEntity.ok(budgetService.deleteBudget(user.getUsername(), id));
    }
}

// ===================== Dashboard Controller =====================
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(defaultValue = "0") int month,
            @RequestParam(defaultValue = "0") int year) {
        int m = month == 0 ? LocalDate.now().getMonthValue() : month;
        int y = year == 0 ? LocalDate.now().getYear() : year;
        return ResponseEntity.ok(dashboardService.getDashboard(user.getUsername(), m, y));
    }
}

// ===================== Notification Controller =====================
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getAll(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notificationService.getNotifications(user.getUsername(), page, size));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> unreadCount(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(notificationService.getUnreadCount(user.getUsername()));
    }

    @PutMapping("/mark-all-read")
    public ResponseEntity<ApiResponse<String>> markAllRead(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(notificationService.markAllRead(user.getUsername()));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<String>> markRead(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markRead(user.getUsername(), id));
    }
}
