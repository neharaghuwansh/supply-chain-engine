package com.supplychain.engine.dto;

import com.supplychain.engine.entity.ExpiryRisk;

import java.math.BigDecimal;

public class ExpiryRiskResponse {

    private Long batchId;
    private String batchNumber;
    private long daysUntilExpiry;
    private ExpiryRisk riskLevel;
    private BigDecimal discountPercent;
    private BigDecimal clearancePrice;

    public ExpiryRiskResponse(Long batchId,
                              String batchNumber,
                              long daysUntilExpiry,
                              ExpiryRisk riskLevel,
                              BigDecimal discountPercent,
                              BigDecimal clearancePrice) {

        this.batchId = batchId;
        this.batchNumber = batchNumber;
        this.daysUntilExpiry = daysUntilExpiry;
        this.riskLevel = riskLevel;
        this.discountPercent = discountPercent;
        this.clearancePrice = clearancePrice;
    }

    public Long getBatchId() {
        return batchId;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public long getDaysUntilExpiry() {
        return daysUntilExpiry;
    }

    public ExpiryRisk getRiskLevel() {
        return riskLevel;
    }

    public BigDecimal getDiscountPercent() {
        return discountPercent;
    }

    public BigDecimal getClearancePrice() {
        return clearancePrice;
    }
}