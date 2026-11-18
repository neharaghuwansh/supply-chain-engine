package com.supplychain.engine.service;

import com.supplychain.engine.entity.Batch;
import com.supplychain.engine.entity.Product;
import com.supplychain.engine.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class LowStockService {

    private final ProductRepository productRepository;

    public LowStockService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public LowStockResult checkLowStock(Long productId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new RuntimeException("Product not found"));

        int sellableStock = 0;

        for (Batch batch : product.getBatches()) {

            boolean hasStock =
                    batch.getQuantity() != null
                            && batch.getQuantity() > 0;

            boolean notExpired =
                    batch.getExpiryDate() != null
                            && batch.getExpiryDate()
                            .isAfter(LocalDate.now());

            if (hasStock && notExpired) {
                sellableStock += batch.getQuantity();
            }
        }

        boolean lowStock =
                sellableStock <= product.getReorderThreshold();

        return new LowStockResult(
                product.getId(),
                product.getName(),
                product.getSku(),
                sellableStock,
                product.getReorderThreshold(),
                lowStock
        );
    }

    public record LowStockResult(
            Long productId,
            String productName,
            String sku,
            int sellableStock,
            int reorderThreshold,
            boolean lowStock
    ) {
    }
}