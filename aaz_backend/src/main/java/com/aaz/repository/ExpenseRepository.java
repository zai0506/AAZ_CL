package com.aaz.repository;

import com.aaz.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByTripGroupIdOrderByExpenseDateDesc(Long tripGroupId);

    List<Expense> findByTripGroupId(Long tripGroupId);

    @Query("SELECT e FROM Expense e WHERE e.tripGroup.id = :tripGroupId AND e.category = :category ORDER BY e.expenseDate DESC")
    List<Expense> findByTripGroupIdAndCategory(Long tripGroupId, String category);
}