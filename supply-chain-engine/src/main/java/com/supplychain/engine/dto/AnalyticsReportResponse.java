package com.supplychain.engine.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class AnalyticsReportResponse {

    private LocalDate startDate;
    private LocalDate endDate;

    private List<SalesTrendPoint> salesTrend;
    private List<TopSellingProduct> topSellingProducts;
    private InventoryMovementSummary inventoryMovement;
    private ExpiryRiskSummary expiryRisk;

    public AnalyticsReportResponse() {
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public List<SalesTrendPoint> getSalesTrend() {
        return salesTrend;
    }

    public void setSalesTrend(List<SalesTrendPoint> salesTrend) {
        this.salesTrend = salesTrend;
    }

    public List<TopSellingProduct> getTopSellingProducts() {
        return topSellingProducts;
    }

    public void setTopSellingProducts(
            List<TopSellingProduct> topSellingProducts) {
        this.topSellingProducts = topSellingProducts;
    }

    public InventoryMovementSummary getInventoryMovement() {
        return inventoryMovement;
    }

    public void setInventoryMovement(
            InventoryMovementSummary inventoryMovement) {
        this.inventoryMovement = inventoryMovement;
    }

    public ExpiryRiskSummary getExpiryRisk() {
        return expiryRisk;
    }

    public void setExpiryRisk(ExpiryRiskSummary expiryRisk) {
        this.expiryRisk = expiryRisk;
    }

    // ==========================================
    // SALES TREND
    // ==========================================

    public static class SalesTrendPoint {

        private LocalDate date;
        private BigDecimal grossRevenue;
        private BigDecimal refundAmount;
        private BigDecimal netRevenue;
        private long numberOfSales;
        private long numberOfReturns;

        public SalesTrendPoint() {
        }

        public SalesTrendPoint(
                LocalDate date,
                BigDecimal grossRevenue,
                BigDecimal refundAmount,
                BigDecimal netRevenue,
                long numberOfSales,
                long numberOfReturns) {

            this.date = date;
            this.grossRevenue = grossRevenue;
            this.refundAmount = refundAmount;
            this.netRevenue = netRevenue;
            this.numberOfSales = numberOfSales;
            this.numberOfReturns = numberOfReturns;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public BigDecimal getGrossRevenue() {
            return grossRevenue;
        }

        public void setGrossRevenue(BigDecimal grossRevenue) {
            this.grossRevenue = grossRevenue;
        }

        public BigDecimal getRefundAmount() {
            return refundAmount;
        }

        public void setRefundAmount(BigDecimal refundAmount) {
            this.refundAmount = refundAmount;
        }

        public BigDecimal getNetRevenue() {
            return netRevenue;
        }

        public void setNetRevenue(BigDecimal netRevenue) {
            this.netRevenue = netRevenue;
        }

        public long getNumberOfSales() {
            return numberOfSales;
        }

        public void setNumberOfSales(long numberOfSales) {
            this.numberOfSales = numberOfSales;
        }

        public long getNumberOfReturns() {
            return numberOfReturns;
        }

        public void setNumberOfReturns(long numberOfReturns) {
            this.numberOfReturns = numberOfReturns;
        }
    }

    // ==========================================
    // TOP SELLING PRODUCTS
    // ==========================================

    public static class TopSellingProduct {

        private Long productId;
        private String productName;
        private long quantitySold;
        private BigDecimal revenue;

        public TopSellingProduct() {
        }

        public TopSellingProduct(
                Long productId,
                String productName,
                long quantitySold,
                BigDecimal revenue) {

            this.productId = productId;
            this.productName = productName;
            this.quantitySold = quantitySold;
            this.revenue = revenue;
        }

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public long getQuantitySold() {
            return quantitySold;
        }

        public void setQuantitySold(long quantitySold) {
            this.quantitySold = quantitySold;
        }

        public BigDecimal getRevenue() {
            return revenue;
        }

        public void setRevenue(BigDecimal revenue) {
            this.revenue = revenue;
        }
    }

    // ==========================================
    // INVENTORY MOVEMENT SUMMARY
    // ==========================================

    public static class InventoryMovementSummary {

        private long purchased;
        private long sold;
        private long returned;
        private long damaged;
        private long expired;
        private long adjusted;

        public InventoryMovementSummary() {
        }

        public long getPurchased() {
            return purchased;
        }

        public void setPurchased(long purchased) {
            this.purchased = purchased;
        }

        public long getSold() {
            return sold;
        }

        public void setSold(long sold) {
            this.sold = sold;
        }

        public long getReturned() {
            return returned;
        }

        public void setReturned(long returned) {
            this.returned = returned;
        }

        public long getDamaged() {
            return damaged;
        }

        public void setDamaged(long damaged) {
            this.damaged = damaged;
        }

        public long getExpired() {
            return expired;
        }

        public void setExpired(long expired) {
            this.expired = expired;
        }

        public long getAdjusted() {
            return adjusted;
        }

        public void setAdjusted(long adjusted) {
            this.adjusted = adjusted;
        }
    }

    // ==========================================
    // EXPIRY RISK SUMMARY
    // ==========================================

    public static class ExpiryRiskSummary {

        private long upcomingExpiryBatches;
        private long upcomingExpiryQuantity;
        private BigDecimal purchaseValueAtRisk;

        public ExpiryRiskSummary() {
        }

        public long getUpcomingExpiryBatches() {
            return upcomingExpiryBatches;
        }

        public void setUpcomingExpiryBatches(
                long upcomingExpiryBatches) {
            this.upcomingExpiryBatches = upcomingExpiryBatches;
        }

        public long getUpcomingExpiryQuantity() {
            return upcomingExpiryQuantity;
        }

        public void setUpcomingExpiryQuantity(
                long upcomingExpiryQuantity) {
            this.upcomingExpiryQuantity = upcomingExpiryQuantity;
        }

        public BigDecimal getPurchaseValueAtRisk() {
            return purchaseValueAtRisk;
        }

        public void setPurchaseValueAtRisk(
                BigDecimal purchaseValueAtRisk) {
            this.purchaseValueAtRisk = purchaseValueAtRisk;
        }
    }
}