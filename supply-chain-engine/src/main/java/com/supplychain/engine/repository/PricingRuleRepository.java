package com.supplychain.engine.repository;

import com.supplychain.engine.entity.ExpiryRisk;
import com.supplychain.engine.entity.PricingRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PricingRuleRepository
        extends JpaRepository<PricingRule, Long> {

    Optional<PricingRule> findByRiskLevel(ExpiryRisk riskLevel);
}