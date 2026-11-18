package com.supplychain.engine.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "pricing_rule",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_pricing_rule_risk_level",
                        columnNames = "risk_level"
                )
        }
)
public class PricingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false)
    private ExpiryRisk riskLevel;

    @Column(
            name = "discount_percent",
            nullable = false,
            precision = 5,
            scale = 2
    )
    private BigDecimal discountPercent;

    @Column(nullable = false)
    private boolean active;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ExpiryRisk getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(ExpiryRisk riskLevel) {
        this.riskLevel = riskLevel;
    }

    public BigDecimal getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(BigDecimal discountPercent) {
        this.discountPercent = discountPercent;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}