package com.supplychain.engine.controller;

import com.supplychain.engine.dto.PricingRuleRequest;
import com.supplychain.engine.entity.PricingRule;
import com.supplychain.engine.repository.PricingRuleRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/pricing-rules")
public class PricingRuleController {

    private final PricingRuleRepository pricingRuleRepository;

    public PricingRuleController(
            PricingRuleRepository pricingRuleRepository) {

        this.pricingRuleRepository = pricingRuleRepository;
    }

    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping
    public List<PricingRule> getAllPricingRules() {

        return pricingRuleRepository.findAll();
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping("/{id}")
    public PricingRule updatePricingRule(
            @PathVariable Long id,
            @RequestBody PricingRuleRequest request) {

        if (request.getDiscountPercent() == null) {
            throw new IllegalArgumentException(
                    "Discount percentage is required"
            );
        }

        if (request.getDiscountPercent().compareTo(BigDecimal.ZERO) < 0
                || request.getDiscountPercent()
                .compareTo(BigDecimal.valueOf(100)) > 0) {

            throw new IllegalArgumentException(
                    "Discount percentage must be between 0 and 100"
            );
        }

        PricingRule rule =
                pricingRuleRepository.findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Pricing rule not found"
                                ));

        rule.setDiscountPercent(
                request.getDiscountPercent()
        );

        return pricingRuleRepository.save(rule);
    }
}