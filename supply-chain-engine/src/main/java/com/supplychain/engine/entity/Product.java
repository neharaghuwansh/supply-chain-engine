package com.supplychain.engine.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String sku;

    private String category;

    private String unit;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal baseSellingPrice;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private Integer reorderThreshold = 20;

    @JsonIgnore
    @OneToMany(mappedBy = "product")
    private List<Batch> batches;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Integer getReorderThreshold() {
        return reorderThreshold;
    }

    public void setReorderThreshold(Integer reorderThreshold) {
        this.reorderThreshold = reorderThreshold;
    }

    public List<Batch> getBatches() {
        return batches;
    }
}