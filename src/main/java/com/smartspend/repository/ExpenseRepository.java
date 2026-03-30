package com.smartspend.repository;

import com.smartspend.entity.Expense;
import com.smartspend.entity.Expense.Category;
import com.smartspend.entity.Expense.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

    Page<Expense> findByUserId(Long userId, Pageable pageable);

    Optional<Expense> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT e FROM Expense e WHERE e.user.id = :userId " +
           "AND (:startDate IS NULL OR e.date >= :startDate) " +
           "AND (:endDate IS NULL OR e.date <= :endDate) " +
           "AND (:category IS NULL OR e.category = :category) " +
           "AND (:type IS NULL OR e.type = :type) " +
           "AND (:minAmount IS NULL OR e.amount >= :minAmount) " +
           "AND (:maxAmount IS NULL OR e.amount <= :maxAmount)")
    Page<Expense> findWithFilters(
        @Param("userId") Long userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("category") Category category,
        @Param("type") TransactionType type,
        @Param("minAmount") BigDecimal minAmount,
        @Param("maxAmount") BigDecimal maxAmount,
        Pageable pageable
    );

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.user.id = :userId AND e.type = :type " +
           "AND MONTH(e.date) = :month AND YEAR(e.date) = :year")
    BigDecimal sumByUserIdAndTypeAndMonthAndYear(
        @Param("userId") Long userId,
        @Param("type") TransactionType type,
        @Param("month") int month,
        @Param("year") int year
    );

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.user.id = :userId AND e.type = 'EXPENSE' " +
           "AND e.category = :category AND MONTH(e.date) = :month AND YEAR(e.date) = :year")
    BigDecimal sumExpenseByUserIdAndCategoryAndMonthAndYear(
        @Param("userId") Long userId,
        @Param("category") Category category,
        @Param("month") int month,
        @Param("year") int year
    );

    @Query("SELECT e.category, COALESCE(SUM(e.amount), 0) FROM Expense e " +
           "WHERE e.user.id = :userId AND e.type = 'EXPENSE' " +
           "AND MONTH(e.date) = :month AND YEAR(e.date) = :year " +
           "GROUP BY e.category")
    List<Object[]> sumExpenseByCategory(
        @Param("userId") Long userId,
        @Param("month") int month,
        @Param("year") int year
    );

    @Query("SELECT MONTH(e.date) as month, COALESCE(SUM(e.amount), 0) as total FROM Expense e " +
           "WHERE e.user.id = :userId AND e.type = :type AND YEAR(e.date) = :year " +
           "GROUP BY MONTH(e.date) ORDER BY MONTH(e.date)")
    List<Object[]> monthlyTotalsByType(
        @Param("userId") Long userId,
        @Param("type") TransactionType type,
        @Param("year") int year
    );

    List<Expense> findByUserIdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT COUNT(e) FROM Expense e WHERE e.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);
}
