package com.aaz.repository;

import com.aaz.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    @Query("SELECT e FROM ExchangeRate e WHERE e.tripGroup.id = :tripGroupId " +
           "AND e.fromCurrency = :fromCurrency AND e.toCurrency = :toCurrency " +
           "ORDER BY e.id DESC LIMIT 1")
    Optional<ExchangeRate> findLatestRate(Long tripGroupId, String fromCurrency, String toCurrency);
}