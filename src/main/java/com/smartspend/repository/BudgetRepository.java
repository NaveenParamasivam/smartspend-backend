package com.smartspend.repository;

import com.smartspend.entity.Budget;
import com.smartspend.entity.Expense.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUserIdAndMonthAndYear(Long userId, int month, int year);
    Optional<Budget> findByUserIdAndCategoryAndMonthAndYear(Long userId, Category category, int month, int year);
    Optional<Budget> findByIdAndUserId(Long id, Long userId);
    List<Budget> findByMonthAndYearAndAlertSent75False(int month, int year);
    List<Budget> findByMonthAndYearAndAlertSent100False(int month, int year);
}
