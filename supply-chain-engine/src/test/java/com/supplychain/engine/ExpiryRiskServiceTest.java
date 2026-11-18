package com.supplychain.engine;

import com.supplychain.engine.entity.ExpiryRisk;
import com.supplychain.engine.service.ExpiryRiskService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExpiryRiskServiceTest {

    private final ExpiryRiskService expiryRiskService = new ExpiryRiskService();

    @Test
    void shouldCalculateCriticalRisk() {

        LocalDate expiryDate = LocalDate.now().plusDays(5);

        ExpiryRisk result = expiryRiskService.calculateRisk(expiryDate);

        assertEquals(ExpiryRisk.CRITICAL, result);
    }
}