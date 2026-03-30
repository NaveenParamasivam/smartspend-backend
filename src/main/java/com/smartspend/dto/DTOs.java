package com.smartspend.dto;

import com.smartspend.entity.Expense.Category;
import com.smartspend.entity.Expense.TransactionType;
import com.smartspend.entity.Notification.NotificationType;
import com.smartspend.entity.User.Role;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class DTOs {

    // ===================== AUTH DTOs =====================

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RegisterRequest {
        @NotBlank(message = "First name is required")
        @Size(min = 2, max = 50)
        private String firstName;

        @NotBlank(message = "Last name is required")
        @Size(min = 2, max = 50)
        private String lastName;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
                message = "Password must contain uppercase, lowercase and number")
        private String password;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LoginRequest {
        @NotBlank @Email
        private String email;
        @NotBlank
        private String password;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AuthResponse {
        private String accessToken;
        @Builder.Default
        private String tokenType = "Bearer";
        private long expiresIn;
        private UserResponse user;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ForgotPasswordRequest {
        @NotBlank @Email
        private String email;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ResetPasswordRequest {
        @NotBlank
        private String token;
        @NotBlank
        @Size(min = 8)
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
                message = "Password must contain uppercase, lowercase and number")
        private String newPassword;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ChangePasswordRequest {
        @NotBlank
        private String currentPassword;
        @NotBlank @Size(min = 8)
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$")
        private String newPassword;
    }

    // ===================== USER DTOs =====================

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class UserResponse {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
        private Role role;
        private boolean enabled;
        private boolean emailVerified;
        private LocalDateTime createdAt;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class UpdateProfileRequest {
        @NotBlank @Size(min = 2, max = 50)
        private String firstName;
        @NotBlank @Size(min = 2, max = 50)
        private String lastName;
    }

    // ===================== EXPENSE DTOs =====================

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ExpenseRequest {
        @NotBlank(message = "Title is required")
        @Size(max = 200)
        private String title;

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        private BigDecimal amount;

        @NotNull(message = "Category is required")
        private Category category;

        @NotNull(message = "Type is required")
        private TransactionType type;

        @NotNull(message = "Date is required")
        private LocalDate date;

        @Size(max = 1000)
        private String description;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ExpenseResponse {
        private Long id;
        private String title;
        private BigDecimal amount;
        private Category category;
        private TransactionType type;
        private LocalDate date;
        private String description;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ExpenseFilterRequest {
        private LocalDate startDate;
        private LocalDate endDate;
        private Category category;
        private TransactionType type;
        private BigDecimal minAmount;
        private BigDecimal maxAmount;
        private String sortBy;
        private String sortDir;
        @Builder.Default
        private int page = 0;
        @Builder.Default
        private int size = 10;
    }

    // ===================== BUDGET DTOs =====================

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class BudgetRequest {
        @NotNull
        private Category category;
        @NotNull @Positive
        private BigDecimal limitAmount;
        @NotNull @Min(1) @Max(12)
        private Integer month;
        @NotNull @Min(2020)
        private Integer year;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class BudgetResponse {
        private Long id;
        private Category category;
        private BigDecimal limitAmount;
        private BigDecimal spentAmount;
        private BigDecimal remainingAmount;
        private double percentageUsed;
        private int month;
        private int year;
        private LocalDateTime createdAt;
    }

    // ===================== DASHBOARD DTOs =====================

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DashboardResponse {
        private BigDecimal totalIncome;
        private BigDecimal totalExpense;
        private BigDecimal netBalance;
        private long totalTransactions;
        private List<CategorySummary> categoryBreakdown;
        private List<MonthlyTrend> monthlyTrends;
        private List<BudgetResponse> budgetStatus;
        private List<ExpenseResponse> recentTransactions;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CategorySummary {
        private Category category;
        private BigDecimal amount;
        private double percentage;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MonthlyTrend {
        private int month;
        private String monthName;
        private BigDecimal income;
        private BigDecimal expense;
    }

    // ===================== NOTIFICATION DTOs =====================

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class NotificationResponse {
        private Long id;
        private String title;
        private String message;
        private NotificationType type;
        private boolean read;
        private LocalDateTime createdAt;
    }

    // ===================== GENERIC =====================

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;

        public static <T> ApiResponse<T> success(String message, T data) {
            return ApiResponse.<T>builder().success(true).message(message).data(data).build();
        }

        public static <T> ApiResponse<T> success(T data) {
            return ApiResponse.<T>builder().success(true).data(data).build();
        }

        public static <T> ApiResponse<T> error(String message) {
            return ApiResponse.<T>builder().success(false).message(message).build();
        }
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PageResponse<T> {
        private List<T> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean last;
    }
}