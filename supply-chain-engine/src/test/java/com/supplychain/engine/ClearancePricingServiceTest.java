package com.supplychain.engine;

import com.supplychain.engine.entity.ExpiryRisk;
import com.supplychain.engine.entity.PricingRule;
import com.supplychain.engine.repository.PricingRuleRepository;
import com.supplychain.engine.service.ClearancePricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClearancePricingServiceTest {

    private PricingRuleRepository pricingRuleRepository;
    private ClearancePricingService pricingService;

    @BeforeEach
    void setUp() {
        pricingRuleRepository = mock(PricingRuleRepository.class);
        pricingService = new ClearancePricingService(pricingRuleRepository);
    }

    private PricingRule createRule(ExpiryRisk risk, String discount) {
        PricingRule rule = new PricingRule();
        rule.setRiskLevel(risk);
        rule.setDiscountPercent(new BigDecimal(discount));
        rule.setActive(true);
        return rule;
    }

    @Test
    void shouldCalculateLowRiskPrice() {
        when(pricingRuleRepository.findByRiskLevel(ExpiryRisk.LOW))
                .thenReturn(Optional.of(createRule(ExpiryRisk.LOW, "0")));

        assertEquals(
                new BigDecimal("25.00"),
                pricingService.calculateClearancePrice(
                        new BigDecimal("25"), ExpiryRisk.LOW)
        );
    }

    @Test
    void shouldCalculateMediumRiskPrice() {
        when(pricingRuleRepository.findByRiskLevel(ExpiryRisk.MEDIUM))
                .thenReturn(Optional.of(createRule(ExpiryRisk.MEDIUM, "10")));

        assertEquals(
                new BigDecimal("90.00"),
                pricingService.calculateClearancePrice(
                        new BigDecimal("100"), ExpiryRisk.MEDIUM)
        );
    }

    @Test
    void shouldCalculateHighRiskPriceUsingConfiguredDiscount() {
        when(pricingRuleRepository.findByRiskLevel(ExpiryRisk.HIGH))
                .thenReturn(Optional.of(createRule(ExpiryRisk.HIGH, "25")));

        assertEquals(
                new BigDecimal("75.00"),
                pricingService.calculateClearancePrice(
                        new BigDecimal("100"), ExpiryRisk.HIGH)
        );
    }

    @Test
    void shouldCalculateCriticalRiskPrice() {
        when(pricingRuleRepository.findByRiskLevel(ExpiryRisk.CRITICAL))
                .thenReturn(Optional.of(createRule(ExpiryRisk.CRITICAL, "40")));

        assertEquals(
                new BigDecimal("60.00"),
                pricingService.calculateClearancePrice(
                        new BigDecimal("100"), ExpiryRisk.CRITICAL)
        );
    }

    @Test
    void shouldReturnZeroPriceForExpiredStock() {
        assertEquals(
                new BigDecimal("0.00"),
                pricingService.calculateClearancePrice(
                        new BigDecimal("100"), ExpiryRisk.EXPIRED)
        );

        verifyNoInteractions(pricingRuleRepository);
    }

    @Test
    void shouldRejectMissingActivePricingRule() {
        when(pricingRuleRepository.findByRiskLevel(ExpiryRisk.CRITICAL))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> pricingService.calculateClearancePrice(
                        new BigDecimal("100"), ExpiryRisk.CRITICAL)
        );
    }

    @Test
    void shouldRejectInactivePricingRule() {
        PricingRule rule = createRule(ExpiryRisk.HIGH, "25");
        rule.setActive(false);

        when(pricingRuleRepository.findByRiskLevel(ExpiryRisk.HIGH))
                .thenReturn(Optional.of(rule));

        assertThrows(
                IllegalStateException.class,
                () -> pricingService.calculateClearancePrice(
                        new BigDecimal("100"), ExpiryRisk.HIGH)
        );
    }

    @Test
    void shouldRejectNullRisk() {
        assertThrows(
                IllegalArgumentException.class,
                () -> pricingService.calculateClearancePrice(
                        new BigDecimal("100"), null)
        );
    }

    @Test
    void shouldRejectNullBasePrice() {
        assertThrows(
                IllegalArgumentException.class,
                () -> pricingService.calculateClearancePrice(
                        null, ExpiryRisk.LOW)
        );
    }

    @Test
    void shouldRejectNegativeBasePrice() {
        assertThrows(
                IllegalArgumentException.class,
                () -> pricingService.calculateClearancePrice(
                        new BigDecimal("-10"), ExpiryRisk.LOW)
        );
    }

    @Test
    void shouldRoundDiscountAmountUsingHalfUp() {
        when(pricingRuleRepository.findByRiskLevel(ExpiryRisk.MEDIUM))
                .thenReturn(Optional.of(createRule(ExpiryRisk.MEDIUM, "10")));

        assertEquals(
                new BigDecimal("9.04"),
                pricingService.calculateClearancePrice(
                        new BigDecimal("10.05"), ExpiryRisk.MEDIUM)
        );
    }
}