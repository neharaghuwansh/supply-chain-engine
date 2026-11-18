package com.supplychain.engine.service;

import com.supplychain.engine.dto.ProductRequest;
import com.supplychain.engine.entity.Product;
import com.supplychain.engine.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // API-based product creation using ProductRequest DTO.
    @Transactional
    public Product createProduct(ProductRequest request) {

        validateProduct(request);

        Product product = new Product();

        product.setName(request.getName());
        product.setSku(request.getSku());
        product.setCategory(request.getCategory());
        product.setUnit(request.getUnit());
        product.setBaseSellingPrice(request.getBaseSellingPrice());

        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }

        product.setReorderThreshold(request.getReorderThreshold());

        return createProduct(product);
    }

    // Internal product creation using Product entity.
// Retained for invoice processing and other existing service callers.
    @Transactional
    public Product createProduct(Product product) {

        validateProduct(product);

        String sku = product.getSku().trim();

        if (productRepository.existsBySku(sku)) {
            throw new IllegalArgumentException(
                    "Product with SKU already exists: " + sku
            );
        }

        product.setName(product.getName().trim());
        product.setSku(sku);

        if (product.getReorderThreshold() == null) {
            product.setReorderThreshold(20);
        }

        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(Long id, ProductRequest request) {

        Product existingProduct = getProductById(id);

        validateProduct(request);

        String updatedSku = request.getSku().trim();

        productRepository.findBySku(updatedSku)
                .filter(product -> !product.getId().equals(id))
                .ifPresent(product -> {
                    throw new IllegalArgumentException(
                            "Product with SKU already exists: " + updatedSku
                    );
                });

        existingProduct.setName(request.getName().trim());
        existingProduct.setSku(updatedSku);
        existingProduct.setCategory(request.getCategory());
        existingProduct.setUnit(request.getUnit());
        existingProduct.setBaseSellingPrice(
                request.getBaseSellingPrice()
        );

        if (request.getActive() != null) {
            existingProduct.setActive(request.getActive());
        }

        // Preserve the existing threshold if omitted from the request.
        if (request.getReorderThreshold() != null) {
            existingProduct.setReorderThreshold(
                    request.getReorderThreshold()
            );
        }

        return productRepository.save(existingProduct);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Product not found with ID: " + id
                        )
                );
    }

    private void validateProduct(ProductRequest request) {

        if (request.getName() == null
                || request.getName().isBlank()) {
            throw new IllegalArgumentException(
                    "Product name is required"
            );
        }

        if (request.getSku() == null
                || request.getSku().isBlank()) {
            throw new IllegalArgumentException(
                    "Product SKU is required"
            );
        }

        if (request.getBaseSellingPrice() == null
                || request.getBaseSellingPrice().signum() <= 0) {
            throw new IllegalArgumentException(
                    "Selling price must be greater than zero"
            );
        }

        if (request.getReorderThreshold() != null
                && request.getReorderThreshold() < 0) {
            throw new IllegalArgumentException(
                    "Reorder threshold cannot be negative"
            );
        }
    }

    private void validateProduct(Product product) {

        if (product.getName() == null
                || product.getName().isBlank()) {
            throw new IllegalArgumentException(
                    "Product name is required"
            );
        }

        if (product.getSku() == null
                || product.getSku().isBlank()) {
            throw new IllegalArgumentException(
                    "Product SKU is required"
            );
        }

        if (product.getBaseSellingPrice() == null
                || product.getBaseSellingPrice().signum() <= 0) {
            throw new IllegalArgumentException(
                    "Selling price must be greater than zero"
            );
        }

        if (product.getReorderThreshold() != null
                && product.getReorderThreshold() < 0) {
            throw new IllegalArgumentException(
                    "Reorder threshold cannot be negative"
            );
        }
    }
}
