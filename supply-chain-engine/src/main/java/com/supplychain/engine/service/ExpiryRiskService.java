package com.supplychain.engine.service;

import com.supplychain.engine.entity.ExpiryRisk;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class ExpiryRiskService {

    public long calculateDaysUntilExpiry(LocalDate expiryDate) {

        LocalDate today = LocalDate.now();

        return ChronoUnit.DAYS.between(today, expiryDate);
    }

    public ExpiryRisk calculateRisk(LocalDate expiryDate) {

        long daysUntilExpiry = calculateDaysUntilExpiry(expiryDate);

        if (daysUntilExpiry <= 0) {
            return ExpiryRisk.EXPIRED;
        } else if (daysUntilExpiry <= 7) {
            return ExpiryRisk.CRITICAL;
        } else if (daysUntilExpiry <= 30) {
            return ExpiryRisk.HIGH;
        } else if (daysUntilExpiry <= 90) {
            return ExpiryRisk.MEDIUM;
        } else {
            return ExpiryRisk.LOW;
        }
    }
}