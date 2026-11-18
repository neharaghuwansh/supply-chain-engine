package com.supplychain.engine.service;

import com.supplychain.engine.dto.ReturnRequest;
import com.supplychain.engine.dto.SaleRequest;
import com.supplychain.engine.entity.*;
import com.supplychain.engine.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final BatchRepository batchRepository;
    private final SaleItemRepository saleItemRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final InventoryTransactionService inventoryTransactionService;
    private final FefoService fefoService;
    private final ExpiryRiskService expiryRiskService;
    private final ClearancePricingService clearancePricingService;

    public SaleService(
            SaleRepository saleRepository,
            ProductRepository productRepository,
            BatchRepository batchRepository,
            SaleItemRepository saleItemRepository,
            InventoryTransactionRepository transactionRepository,
            InventoryTransactionService inventoryTransactionService,
            FefoService fefoService,
            ExpiryRiskService expiryRiskService,
            ClearancePricingService clearancePricingService) {

        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.batchRepository = batchRepository;
        this.saleItemRepository = saleItemRepository;
        this.transactionRepository = transactionRepository;
        this.inventoryTransactionService = inventoryTransactionService;
        this.fefoService = fefoService;
        this.expiryRiskService = expiryRiskService;
        this.clearancePricingService = clearancePricingService;
    }

    @Transactional
    public Sale createSale(SaleRequest request) {

        if (request == null
                || request.getIdempotencyKey() == null
                || request.getIdempotencyKey().isBlank()) {
            throw new IllegalArgumentException(
                    "Checkout request ID is required");
        }

        String idempotencyKey = request.getIdempotencyKey().trim();

        UUID requestUuid;

        try {
            requestUuid = UUID.fromString(idempotencyKey);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Invalid checkout request ID");
        }

        // Acquire a transaction-level PostgreSQL advisory lock.
        // Requests using the same UUID must wait for one another.
        long lockId = requestUuid.getMostSignificantBits()
                ^ requestUuid.getLeastSignificantBits();

        saleRepository.acquireIdempotencyLock(lockId);

        // Check again after acquiring the lock.
        // A previous request may have completed while this request waited.
        Sale existingSale = saleRepository
                .findByIdempotencyKey(idempotencyKey)
                .orElse(null);

        if (existingSale != null) {
            return existingSale;
        }

        if (request.getItems() == null
                || request.getItems().isEmpty()) {
            throw new IllegalArgumentException(
                    "Sale must contain at least one item");
        }

        Map<Long, Integer> requestedQuantities = new LinkedHashMap<>();

        for (SaleRequest.SaleItemRequest item : request.getItems()) {

            if (item == null
                    || item.getProductId() == null
                    || item.getQuantity() == null
                    || item.getQuantity() <= 0) {
                throw new IllegalArgumentException(
                        "Each item must have a valid product ID and positive quantity");
            }

            try {
                requestedQuantities.merge(
                        item.getProductId(),
                        item.getQuantity(),
                        Math::addExact
                );
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException(
                        "Requested quantity is too large for product ID: "
                                + item.getProductId(),
                        exception
                );
            }
        }

        Sale sale = new Sale();
        sale.setSaleDate(LocalDateTime.now());
        sale.setIdempotencyKey(idempotencyKey);

        List<SaleItem> saleItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry
                : requestedQuantities.entrySet()) {

            Long productId = entry.getKey();
            int requestedQuantity = entry.getValue();

            Product product = productRepository.findById(productId)
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Product not found: " + productId));

            if (!product.isActive()) {
                throw new IllegalStateException(
                        "Product is inactive: " + product.getName());
            }

            if (product.getBaseSellingPrice() == null
                    || product.getBaseSellingPrice()
                    .compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalStateException(
                        "Product has an invalid base selling price: "
                                + product.getName());
            }

            List<FefoService.BatchAllocation> allocations =
                    fefoService.allocateStock(
                            productId,
                            requestedQuantity
                    );

            if (allocations == null || allocations.isEmpty()) {
                throw new IllegalStateException(
                        "No eligible stock available for product: "
                                + product.getName());
            }

            int allocatedQuantity = 0;

            for (FefoService.BatchAllocation allocation : allocations) {

                Batch batch = batchRepository
                        .findById(allocation.batchId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Batch not found: "
                                                + allocation.batchId()));

                if (batch.isArchived()) {
                    throw new IllegalStateException(
                            "Archived batch cannot be sold: "
                                    + batch.getBatchNumber());
                }

                if (!batch.getProduct().getId().equals(productId)) {
                    throw new IllegalStateException(
                            "Allocated batch does not belong to product ID: "
                                    + productId);
                }

                if (allocation.quantity() <= 0) {
                    throw new IllegalStateException(
                            "FEFO returned an invalid allocation quantity");
                }

                if (batch.getExpiryDate() == null
                        || !batch.getExpiryDate().isAfter(LocalDate.now())) {
                    throw new IllegalStateException(
                            "Expired batches cannot be sold: "
                                    + batch.getBatchNumber());
                }

                ExpiryRisk risk = expiryRiskService.calculateRisk(
                        batch.getExpiryDate()
                );

                if (risk == ExpiryRisk.EXPIRED) {
                    throw new IllegalStateException(
                            "Expired batches cannot be sold: "
                                    + batch.getBatchNumber());
                }

                BigDecimal sellingPrice =
                        clearancePricingService.calculateClearancePrice(
                                product.getBaseSellingPrice(),
                                risk
                        );

                if (sellingPrice == null
                        || sellingPrice.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalStateException(
                            "Calculated selling price is invalid for batch: "
                                    + batch.getBatchNumber());
                }

                int quantity = allocation.quantity();

                SaleItem saleItem = new SaleItem();
                saleItem.setSale(sale);
                saleItem.setProduct(product);
                saleItem.setBatch(batch);
                saleItem.setQuantity(quantity);

                saleItem.setSellingPrice(
                        sellingPrice.setScale(2, RoundingMode.HALF_UP)
                );

                saleItems.add(saleItem);

                totalAmount = totalAmount.add(
                        saleItem.getSellingPrice()
                                .multiply(BigDecimal.valueOf(quantity))
                );

                try {
                    allocatedQuantity = Math.addExact(
                            allocatedQuantity,
                            quantity
                    );
                } catch (ArithmeticException exception) {
                    throw new IllegalStateException(
                            "Total allocated quantity is too large",
                            exception
                    );
                }
            }

            if (allocatedQuantity != requestedQuantity) {
                throw new IllegalStateException(
                        "FEFO allocation did not match the requested quantity "
                                + "for product: " + product.getName()
                                + ". Requested: " + requestedQuantity
                                + ", allocated: " + allocatedQuantity);
            }
        }

        sale.setItems(saleItems);
        sale.setTotalAmount(
                totalAmount.setScale(2, RoundingMode.HALF_UP)
        );

        Sale savedSale = saleRepository.saveAndFlush(sale);

        for (SaleItem item : savedSale.getItems()) {
            inventoryTransactionService.recordSaleTransaction(
                    item.getBatch().getId(),
                    item.getQuantity(),
                    "SALE-" + savedSale.getId(),
                    item
            );
        }

        return savedSale;
    }

    @Transactional(readOnly = true)
    public Sale getSaleById(Long id) {
        return saleRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Sale not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Sale> getAllSales() {
        return saleRepository.findAll(
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC,
                        "saleDate"
                )
        );
    }

    @Transactional(readOnly = true)
    public Page<Sale> searchSales(
            String search,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {

        String normalizedSearch =
                search == null || search.isBlank()
                        ? null
                        : search.trim();

        return saleRepository.searchSales(
                normalizedSearch,
                startDate,
                endDate,
                pageable
        );
    }

    @Transactional
    public InventoryTransaction processReturn(ReturnRequest request) {

        if (request == null
                || request.getSaleId() == null
                || request.getSaleItemId() == null
                || request.getQuantity() == null
                || request.getQuantity() <= 0) {
            throw new IllegalArgumentException(
                    "Sale ID, sale item ID, and a positive return quantity are required");
        }

        SaleItem saleItem = saleItemRepository
                .findByIdForUpdate(request.getSaleItemId())
                .orElseThrow(() ->
                        new RuntimeException("Sale item not found"));

        if (saleItem.getSale() == null
                || saleItem.getSale().getId() == null
                || !saleItem.getSale().getId().equals(request.getSaleId())) {
            throw new IllegalArgumentException(
                    "Sale item does not belong to the specified sale");
        }

        Integer previouslyReturned =
                transactionRepository.sumQuantityBySaleItemIdAndType(
                        saleItem.getId(),
                        InventoryTransactionType.RETURN
                );

        int alreadyReturned = previouslyReturned == null
                ? 0
                : previouslyReturned;

        int remainingReturnable =
                saleItem.getQuantity() - alreadyReturned;

        if (remainingReturnable <= 0
                || request.getQuantity() > remainingReturnable) {
            throw new IllegalArgumentException(
                    "Return quantity exceeds remaining returnable quantity: "
                            + Math.max(remainingReturnable, 0));
        }

        return inventoryTransactionService.recordReturnTransaction(
                saleItem.getBatch().getId(),
                request.getQuantity(),
                "CUSTOMER-RETURN-SALE-" + request.getSaleId(),
                saleItem
        );
    }

    @Transactional(readOnly = true)
    public List<InventoryTransaction> getSaleReturns(Long saleId) {

        if (!saleRepository.existsById(saleId)) {
            throw new RuntimeException("Sale not found: " + saleId);
        }

        return transactionRepository.findReturnsBySaleIdAndType(
                saleId,
                InventoryTransactionType.RETURN
        );
    }
}