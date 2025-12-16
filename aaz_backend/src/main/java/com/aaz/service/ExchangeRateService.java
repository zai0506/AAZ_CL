package com.aaz.service;

import com.aaz.entity.ExchangeRate;
import com.aaz.entity.TripGroup;
import com.aaz.repository.ExchangeRateRepository;
import com.aaz.repository.TripGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;
    private final TripGroupRepository tripGroupRepository;

    public BigDecimal convert(Long tripId, BigDecimal amount,
            String fromCurrency, String toCurrency,
            BigDecimal manualRate) {
        if (fromCurrency.equals(toCurrency)) {
            return amount;
        }

        BigDecimal rate;
        if (manualRate != null && manualRate.compareTo(BigDecimal.ZERO) > 0) {
            rate = manualRate;
            saveRate(tripId, fromCurrency, toCurrency, rate);
        } else {
            rate = getRate(tripId, fromCurrency, toCurrency);
        }

        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getRate(Long tripId, String fromCurrency, String toCurrency) {
        // 同一貨幣直接回傳 1
        if (fromCurrency.equals(toCurrency)) {
            return BigDecimal.ONE;
        }

        // 查詢資料庫，查不到時回傳 null（不要回傳 BigDecimal.ONE）
        return exchangeRateRepository
                .findLatestRate(tripId, fromCurrency, toCurrency)
                .map(ExchangeRate::getRate)
                .orElse(null);
    }

    // ========== 新增：給 TransactionService 用的方法（帶 date 參數）==========
    public BigDecimal getRate(String fromCurrency, String toCurrency, LocalDate date) {
        // 這個方法暫時忽略 date，使用最新匯率
        // 如果需要根據日期查詢，可以修改 Repository 的查詢邏輯
        return BigDecimal.ONE; // 預設返回 1，表示需要手動輸入匯率
    }

    // ========== 新增：給 TransactionService 用的方法（帶 date 參數）==========
    public void saveRate(String fromCurrency, String toCurrency, LocalDate date, BigDecimal rate) {
        // 這個方法暫時不儲存，因為原本的設計是綁定 tripId
        // 如果要儲存，需要額外處理
    }

    public void saveRate(Long tripId, String from, String to, BigDecimal rate) {
        TripGroup trip = tripGroupRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("群組不存在"));

        // 查詢是否已存在該組合的匯率
        ExchangeRate existingRate = exchangeRateRepository
                .findLatestRate(tripId, from, to)
                .orElse(null);

        if (existingRate != null) {
            // 更新現有紀錄
            existingRate.setRate(rate);
            exchangeRateRepository.save(existingRate);
        } else {
            // 建立新紀錄
            ExchangeRate er = new ExchangeRate();
            er.setTripGroup(trip);
            er.setFromCurrency(from);
            er.setToCurrency(to);
            er.setRate(rate);
            exchangeRateRepository.save(er);
        }
    }
}