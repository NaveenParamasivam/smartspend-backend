package com.smartspend.controller;

import com.smartspend.dto.DTOs.*;
import com.smartspend.entity.Expense;
import com.smartspend.service.ExpenseService;
import com.smartspend.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;
    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<ApiResponse<ExpenseResponse>> create(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.ok(expenseService.createExpense(user.getUsername(), request));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ExpenseResponse>>> getAll(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Expense.Category category,
            @RequestParam(required = false) Expense.TransactionType type,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        ExpenseFilterRequest filter = ExpenseFilterRequest.builder()
            .startDate(startDate).endDate(endDate).category(category).type(type)
            .minAmount(minAmount).maxAmount(maxAmount)
            .sortBy(sortBy).sortDir(sortDir).page(page).size(size).build();

        return ResponseEntity.ok(expenseService.getExpenses(user.getUsername(), filter));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpenseResponse>> getById(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id) {
        return ResponseEntity.ok(expenseService.getExpenseById(user.getUsername(), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpenseResponse>> update(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id,
            @Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.ok(expenseService.updateExpense(user.getUsername(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id) {
        return ResponseEntity.ok(expenseService.deleteExpense(user.getUsername(), id));
    }

    @GetMapping("/report/pdf")
    public ResponseEntity<byte[]> downloadPdf(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<Expense> expenses = expenseService.getExpensesForReport(user.getUsername(), from, to);
        byte[] pdf = reportService.generatePdfReport(expenses, from, to, user.getUsername());
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=smartspend-report.pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }

    @GetMapping("/report/excel")
    public ResponseEntity<byte[]> downloadExcel(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) throws Exception {
        List<Expense> expenses = expenseService.getExpensesForReport(user.getUsername(), from, to);
        byte[] excel = reportService.generateExcelReport(expenses, from, to, user.getUsername());
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=smartspend-report.xlsx")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(excel);
    }
}
