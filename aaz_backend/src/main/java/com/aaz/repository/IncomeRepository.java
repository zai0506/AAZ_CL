package com.aaz.repository;

import com.aaz.entity.Income;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IncomeRepository extends JpaRepository<Income, Long> {
    List<Income> findByTripGroupIdOrderByIncomeDateDesc(Long tripGroupId);

    List<Income> findByTripGroupId(Long tripGroupId);

    @Query("SELECT i FROM Income i WHERE i.tripGroup.id = :tripGroupId AND i.category = :category ORDER BY i.incomeDate DESC")
    List<Income> findByTripGroupIdAndCategory(Long tripGroupId, String category);
}