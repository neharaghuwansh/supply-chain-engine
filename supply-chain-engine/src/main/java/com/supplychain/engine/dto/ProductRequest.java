package com.supplychain.engine.dto;

import java.math.BigDecimal;

public class ProductRequest {

    private String name;
    private String sku;
    private String category;
    private String unit;
    private BigDecimal baseSellingPrice;
    private Boolean active;
    private Integer reorderThreshold;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getBaseSellingPrice() {
        return baseSellingPrice;
    }

    public void setBaseSellingPrice(BigDecimal baseSellingPrice) {
        this.baseSellingPrice = baseSellingPrice;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Integer getReorderThreshold() {
        return reorderThreshold;
    }

    public void setReorderThreshold(Integer reorderThreshold) {
        this.reorderThreshold = reorderThreshold;
    }
}