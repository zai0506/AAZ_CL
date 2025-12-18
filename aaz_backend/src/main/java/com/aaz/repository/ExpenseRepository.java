package com.aaz.repository;

import com.aaz.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByTripGroupIdOrderByExpenseDateDesc(Long tripGroupId);

    List<Expense> findByTripGroupId(Long tripGroupId);

    @Query("SELECT e FROM Expense e WHERE e.tripGroup.id = :tripGroupId AND e.category = :category ORDER BY e.expenseDate DESC")
    List<Expense> findByTripGroupIdAndCategory(Long tripGroupId, String category);

    @Query("SELECT e FROM Expense e WHERE e.tripGroup.id = :tripGroupId " +
           "AND (:startDate IS NULL OR e.expenseDate >= :startDate) " +
           "AND (:endDate IS NULL OR e.expenseDate <= :endDate)")
    List<Expense> findByTripGroupIdAndDateRange(
        @Param("tripGroupId") Long tripGroupId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}