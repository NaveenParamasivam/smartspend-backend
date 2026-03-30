package com.smartspend;

import com.smartspend.dto.DTOs.*;
import com.smartspend.entity.Expense;
import com.smartspend.entity.User;
import com.smartspend.exception.AppException;
import com.smartspend.repository.ExpenseRepository;
import com.smartspend.repository.UserRepository;
import com.smartspend.service.*;
import com.smartspend.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmartSpendTests {

    @Nested
    class AuthServiceTests {
        @Mock UserRepository userRepository;
        @Mock PasswordEncoder passwordEncoder;
        @Mock JwtUtil jwtUtil;
        @Mock AuthenticationManager authManager;
        @Mock EmailService emailService;
        @InjectMocks AuthService authService;

        @Test
        void register_success() {
            RegisterRequest req = new RegisterRequest("John","Doe","john@test.com","Password1");
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("hash");
            when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            doNothing().when(emailService).sendVerificationEmail(any(),any(),any());
            assertThat(authService.register(req).isSuccess()).isTrue();
        }

        @Test
        void register_duplicateEmail_throws() {
            when(userRepository.existsByEmail(anyString())).thenReturn(true);
            assertThatThrownBy(() -> authService.register(new RegisterRequest("J","D","j@t.com","Pass1")))
                .isInstanceOf(AppException.class);
        }

        @Test
        void login_badCredentials_throws() {
            when(authManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
            assertThatThrownBy(() -> authService.login(new LoginRequest("x@t.com","bad")))
                .isInstanceOf(AppException.class).hasMessageContaining("Invalid");
        }

        @Test
        void forgotPassword_unknownEmail_stillReturnsSuccess() {
            when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
            assertThat(authService.forgotPassword(new ForgotPasswordRequest("x@t.com")).isSuccess()).isTrue();
        }

        @Test
        void verifyEmail_expiredToken_throws() {
            User u = User.builder().verificationToken("tok")
                .verificationTokenExpiry(java.time.LocalDateTime.now().minusHours(1)).build();
            when(userRepository.findByVerificationToken("tok")).thenReturn(Optional.of(u));
            assertThatThrownBy(() -> authService.verifyEmail("tok"))
                .isInstanceOf(AppException.class).hasMessageContaining("expired");
        }
    }

    @Nested
    class ExpenseServiceTests {
        @Mock ExpenseRepository expenseRepository;
        @Mock UserRepository userRepository;
        @Mock BudgetService budgetService;
        @InjectMocks ExpenseService expenseService;
        User user;

        @BeforeEach void setup() {
            user = User.builder().id(1L).email("t@t.com").build();
        }

        @Test
        void createExpense_success() {
            ExpenseRequest req = ExpenseRequest.builder().title("Lunch")
                .amount(BigDecimal.TEN).category(Expense.Category.FOOD)
                .type(Expense.TransactionType.EXPENSE).date(LocalDate.now()).build();
            when(userRepository.findByEmail("t@t.com")).thenReturn(Optional.of(user));
            Expense saved = Expense.builder().id(1L).title("Lunch").amount(BigDecimal.TEN)
                .category(Expense.Category.FOOD).type(Expense.TransactionType.EXPENSE)
                .date(LocalDate.now()).user(user).build();
            when(expenseRepository.save(any())).thenReturn(saved);
            assertThat(expenseService.createExpense("t@t.com", req).isSuccess()).isTrue();
        }

        @Test
        void deleteExpense_notFound_throws() {
            when(userRepository.findByEmail("t@t.com")).thenReturn(Optional.of(user));
            when(expenseRepository.findByIdAndUserId(99L,1L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> expenseService.deleteExpense("t@t.com", 99L))
                .isInstanceOf(AppException.class);
        }

        @Test
        void getById_found_returnsResponse() {
            Expense e = Expense.builder().id(3L).title("Bus").amount(BigDecimal.valueOf(50))
                .category(Expense.Category.TRANSPORT).type(Expense.TransactionType.EXPENSE)
                .date(LocalDate.now()).user(user).build();
            when(userRepository.findByEmail("t@t.com")).thenReturn(Optional.of(user));
            when(expenseRepository.findByIdAndUserId(3L,1L)).thenReturn(Optional.of(e));
            assertThat(expenseService.getExpenseById("t@t.com", 3L).getData().getId()).isEqualTo(3L);
        }
    }
}
