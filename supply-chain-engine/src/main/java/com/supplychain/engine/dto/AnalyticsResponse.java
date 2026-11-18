package com.supplychain.engine.dto;

import java.math.BigDecimal;

public class AnalyticsResponse {

    private long totalProducts;
    private long totalBatches;
    private long totalInventoryQuantity;

    private long expiredBatches;
    private long nearExpiryBatches;
    private long lowStockProducts;

    private long totalSales;
    private BigDecimal totalSalesRevenue;

    private BigDecimal totalInventoryPurchaseValue;

    public AnalyticsResponse() {
    }

    public long getTotalProducts() {
        return totalProducts;
    }

    public void setTotalProducts(long totalProducts) {
        this.totalProducts = totalProducts;
    }

    public long getTotalBatches() {
        return totalBatches;
    }

    public void setTotalBatches(long totalBatches) {
        this.totalBatches = totalBatches;
    }

    public long getTotalInventoryQuantity() {
        return totalInventoryQuantity;
    }

    public void setTotalInventoryQuantity(long totalInventoryQuantity) {
        this.totalInventoryQuantity = totalInventoryQuantity;
    }

    public long getExpiredBatches() {
        return expiredBatches;
    }

    public void setExpiredBatches(long expiredBatches) {
        this.expiredBatches = expiredBatches;
    }

    public long getNearExpiryBatches() {
        return nearExpiryBatches;
    }

    public void setNearExpiryBatches(long nearExpiryBatches) {
        this.nearExpiryBatches = nearExpiryBatches;
    }

    public long getLowStockProducts() {
        return lowStockProducts;
    }

    public void setLowStockProducts(long lowStockProducts) {
        this.lowStockProducts = lowStockProducts;
    }

    public long getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(long totalSales) {
        this.totalSales = totalSales;
    }

    public BigDecimal getTotalSalesRevenue() {
        return totalSalesRevenue;
    }

    public void setTotalSalesRevenue(BigDecimal totalSalesRevenue) {
        this.totalSalesRevenue = totalSalesRevenue;
    }

    public BigDecimal getTotalInventoryPurchaseValue() {
        return totalInventoryPurchaseValue;
    }

    public void setTotalInventoryPurchaseValue(
            BigDecimal totalInventoryPurchaseValue) {

        this.totalInventoryPurchaseValue =
                totalInventoryPurchaseValue;
    }
}