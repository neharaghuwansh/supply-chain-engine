package com.supplychain.engine.service;

import com.supplychain.engine.entity.ExpiryRisk;
import com.supplychain.engine.entity.PricingRule;
import com.supplychain.engine.repository.PricingRuleRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ClearancePricingService {

    private final PricingRuleRepository pricingRuleRepository;

    public ClearancePricingService(
            PricingRuleRepository pricingRuleRepository) {
        this.pricingRuleRepository = pricingRuleRepository;
    }

    public BigDecimal calculateDiscountPercent(ExpiryRisk risk) {

        if (risk == null) {
            throw new IllegalArgumentException(
                    "Expiry risk cannot be null"
            );
        }

        // Expired batches can be evaluated for reporting,
        // but must never be sold.
        if (risk == ExpiryRisk.EXPIRED) {
            return BigDecimal.valueOf(100);
        }

        PricingRule rule = pricingRuleRepository
                .findByRiskLevel(risk)
                .filter(PricingRule::isActive)
                .orElseThrow(() -> new IllegalStateException(
                        "No active pricing rule found for risk level: "
                                + risk
                ));

        return rule.getDiscountPercent();
    }

    public BigDecimal calculateClearancePrice(
            BigDecimal baseSellingPrice,
            ExpiryRisk risk) {

        if (baseSellingPrice == null
                || baseSellingPrice.signum() < 0) {
            throw new IllegalArgumentException(
                    "Base selling price cannot be null or negative"
            );
        }

        BigDecimal discountPercent =
                calculateDiscountPercent(risk);

        BigDecimal discountAmount = baseSellingPrice
                .multiply(discountPercent)
                .divide(
                        BigDecimal.valueOf(100),
                        2,
                        RoundingMode.HALF_UP
                );

        return baseSellingPrice
                .subtract(discountAmount)
                .setScale(2, RoundingMode.HALF_UP);
    }
}