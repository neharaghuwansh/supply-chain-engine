package com.supplychain.engine.controller;

import com.supplychain.engine.entity.Product;
import com.supplychain.engine.repository.ProductRepository;
import com.supplychain.engine.service.LowStockService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/low-stock")
public class LowStockController {

    private final LowStockService lowStockService;
    private final ProductRepository productRepository;

    public LowStockController(
            LowStockService lowStockService,
            ProductRepository productRepository) {

        this.lowStockService = lowStockService;
        this.productRepository = productRepository;
    }

    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/{productId}")
    public LowStockService.LowStockResult checkLowStock(
            @PathVariable Long productId) {

        return lowStockService.checkLowStock(productId);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping
    public List<LowStockService.LowStockResult> getLowStockProducts() {

        List<LowStockService.LowStockResult> lowStockProducts =
                new ArrayList<>();

        List<Product> products =
                productRepository.findAll();

        for (Product product : products) {

            LowStockService.LowStockResult result =
                    lowStockService.checkLowStock(product.getId());

            if (result.lowStock()) {
                lowStockProducts.add(result);
            }
        }

        return lowStockProducts;
    }
}