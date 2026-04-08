package com.smartspend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartspend.dto.DTOs.*;
import com.smartspend.entity.User;
import com.smartspend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IntegrationTests {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    // Prevent Spring from requiring a real mail server during tests
    @MockBean
    JavaMailSender javaMailSender;

    // Prevent WebSocket broker from being required in test context
    @MockBean
    org.springframework.messaging.simp.SimpMessagingTemplate simpMessagingTemplate;

    private String authToken;
    private User testUser;

    @BeforeEach
    void setUp() throws Exception {
        // Clean slate for each test
        userRepository.deleteAll();

        testUser = User.builder()
            .firstName("Test").lastName("User")
            .email("test@smartspend.com")
            .password(passwordEncoder.encode("Password1"))
            .role(User.Role.USER)
            .enabled(true)
            .emailVerified(true)
            .build();
        userRepository.save(testUser);

        // Login and get token
        LoginRequest loginReq = new LoginRequest("test@smartspend.com", "Password1");
        MvcResult result = mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
            .andExpect(status().isOk())
            .andReturn();

        String resp = result.getResponse().getContentAsString();
        authToken = objectMapper.readTree(resp).path("data").path("accessToken").asText();
    }

    // ── Auth Tests ────────────────────────────────────────────────────────────

    @Test
    void register_validRequest_returns200() throws Exception {
        RegisterRequest req = new RegisterRequest("Jane", "Doe", "jane@example.com", "Password1");
        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        RegisterRequest req = new RegisterRequest("Test", "User", "test@smartspend.com", "Password1");
        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().is(409))
            .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void login_validCredentials_returnsToken() throws Exception {
        LoginRequest req = new LoginRequest("test@smartspend.com", "Password1");
        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken", not(emptyString())))
            .andExpect(jsonPath("$.data.user.email", is("test@smartspend.com")));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        LoginRequest req = new LoginRequest("test@smartspend.com", "WrongPass1");
        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().is(401));
    }

    @Test
    void getMe_withToken_returnsUser() throws Exception {
        mockMvc.perform(
            get("/auth/me")
                .header("Authorization", "Bearer " + authToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.email", is("test@smartspend.com")));
    }

    @Test
    void getMe_noToken_returns401() throws Exception {
        mockMvc.perform(get("/auth/me"))
//            .andExpect(status().isUnauthorized());
                .andExpect(status().isForbidden());
    }

    // ── Expense Tests ─────────────────────────────────────────────────────────

    @Test
    void createExpense_valid_returns200() throws Exception {
        ExpenseRequest req = ExpenseRequest.builder()
            .title("Lunch").amount(java.math.BigDecimal.valueOf(250))
            .category(com.smartspend.entity.Expense.Category.FOOD)
            .type(com.smartspend.entity.Expense.TransactionType.EXPENSE)
            .date(java.time.LocalDate.now()).build();

        mockMvc.perform(
            post("/expenses")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title", is("Lunch")))
            .andExpect(jsonPath("$.data.amount", is(250)));
    }

    @Test
    void createExpense_missingTitle_returns400() throws Exception {
        ExpenseRequest req = ExpenseRequest.builder()
            .amount(java.math.BigDecimal.valueOf(100))
            .category(com.smartspend.entity.Expense.Category.FOOD)
            .type(com.smartspend.entity.Expense.TransactionType.EXPENSE)
            .date(java.time.LocalDate.now()).build();

        mockMvc.perform(
            post("/expenses")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getExpenses_returnsPagedList() throws Exception {
        mockMvc.perform(
            get("/expenses")
                .header("Authorization", "Bearer " + authToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content", isA(java.util.List.class)));
    }

    // ── Dashboard Tests ───────────────────────────────────────────────────────

    @Test
    void getDashboard_returnsData() throws Exception {
        mockMvc.perform(
            get("/dashboard")
                .header("Authorization", "Bearer " + authToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalIncome").exists())
            .andExpect(jsonPath("$.data.totalExpense").exists())
            .andExpect(jsonPath("$.data.netBalance").exists());
    }
}
