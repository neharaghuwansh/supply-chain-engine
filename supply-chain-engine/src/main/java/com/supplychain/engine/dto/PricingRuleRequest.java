package com.supplychain.engine.dto;

import java.math.BigDecimal;

public class PricingRuleRequest {

    private BigDecimal discountPercent;

    public BigDecimal getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(BigDecimal discountPercent) {
        this.discountPercent = discountPercent;
    }
}