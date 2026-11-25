package com.supplychain.engine.service;

import com.supplychain.engine.dto.BatchRequest;
import com.supplychain.engine.dto.ExpiryRiskResponse;
import com.supplychain.engine.entity.Batch;
import com.supplychain.engine.entity.ExpiryRisk;
import com.supplychain.engine.entity.Product;
import com.supplychain.engine.repository.BatchRepository;
import com.supplychain.engine.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class BatchService {

    private final BatchRepository batchRepository;
    private final ProductRepository productRepository;
    private final ExpiryRiskService expiryRiskService;
    private final ClearancePricingService clearancePricingService;
    private final InventoryTransactionService inventoryTransactionService;

    public BatchService(
            BatchRepository batchRepository,
            ProductRepository productRepository,
            ExpiryRiskService expiryRiskService,
            ClearancePricingService clearancePricingService,
            InventoryTransactionService inventoryTransactionService) {

        this.batchRepository = batchRepository;
        this.productRepository = productRepository;
        this.expiryRiskService = expiryRiskService;
        this.clearancePricingService = clearancePricingService;
        this.inventoryTransactionService = inventoryTransactionService;
    }

    @Transactional
    public Batch createBatch(BatchRequest request) {

        validateBatchRequest(request);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() ->
                        new RuntimeException("Product not found"));

        Batch batch = new Batch();
        batch.setProduct(product);
        batch.setBatchNumber(request.getBatchNumber().trim());
        batch.setManufacturingDate(request.getManufacturingDate());
        batch.setExpiryDate(request.getExpiryDate());
        batch.setQuantity(0);
        batch.setPurchasePrice(request.getPurchasePrice());
        batch.setReceivedDate(request.getReceivedDate());
        batch.setArchived(false);

        Batch savedBatch = batchRepository.save(batch);

        inventoryTransactionService.recordTransaction(
                savedBatch.getId(),
                com.supplychain.engine.entity.InventoryTransactionType.PURCHASE,
                request.getQuantity(),
                "BATCH-RECEIPT"
        );

        return getBatchById(savedBatch.getId());
    }

    public List<Batch> getAllBatches() {
        return batchRepository.findAll();
    }

    public Batch getBatchById(Long id) {
        return batchRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Batch not found"));
    }

    @Transactional
    public Batch updateBatch(Long id, BatchRequest request) {

        validateBatchRequestForUpdate(request);

        Batch batch = batchRepository.findByIdForUpdate(id)
                .orElseThrow(() ->
                        new RuntimeException("Batch not found"));

        if (batch.isArchived()) {
            throw new IllegalStateException(
                    "Archived batches cannot be edited"
            );
        }

        // Expired batches cannot be edited through ordinary batch editing.
        if (batch.getExpiryDate() != null
                && batch.getExpiryDate().isBefore(LocalDate.now())) {
            throw new IllegalStateException(
                    "Expired batches cannot be edited. "
                            + "Expiry corrections require a separate authorized process."
            );
        }

        // Preserve the existing product association and stock quantity.
        batch.setBatchNumber(request.getBatchNumber().trim());
        batch.setManufacturingDate(request.getManufacturingDate());
        batch.setExpiryDate(request.getExpiryDate());
        batch.setPurchasePrice(request.getPurchasePrice());
        batch.setReceivedDate(request.getReceivedDate());

        return batchRepository.save(batch);
    }

    @Transactional
    public Batch archiveBatch(Long id) {

        Batch batch = batchRepository.findByIdForUpdate(id)
                .orElseThrow(() ->
                        new RuntimeException("Batch not found"));

        if (batch.isArchived()) {
            throw new IllegalStateException(
                    "Batch is already archived"
            );
        }

        if (batch.getQuantity() != 0) {
            throw new IllegalStateException(
                    "Only batches with zero stock can be archived. "
                            + "Current quantity: " + batch.getQuantity()
            );
        }

        batch.setArchived(true);

        return batchRepository.save(batch);
    }

    @Transactional
    public Batch restoreBatch(Long id) {

        Batch batch = batchRepository.findByIdForUpdate(id)
                .orElseThrow(() ->
                        new RuntimeException("Batch not found"));

        if (!batch.isArchived()) {
            throw new IllegalStateException(
                    "Batch is not archived"
            );
        }

        // Restore the record only. Never add stock automatically.
        batch.setArchived(false);

        return batchRepository.save(batch);
    }

    public ExpiryRiskResponse getExpiryRisk(Long batchId) {

        Batch batch = getBatchById(batchId);

        long daysUntilExpiry =
                expiryRiskService.calculateDaysUntilExpiry(
                        batch.getExpiryDate()
                );

        ExpiryRisk risk =
                expiryRiskService.calculateRisk(
                        batch.getExpiryDate()
                );

        BigDecimal discountPercent =
                clearancePricingService.calculateDiscountPercent(risk);

        BigDecimal clearancePrice =
                clearancePricingService.calculateClearancePrice(
                        batch.getProduct().getBaseSellingPrice(),
                        risk
                );

        return new ExpiryRiskResponse(
                batch.getId(),
                batch.getBatchNumber(),
                daysUntilExpiry,
                risk,
                discountPercent,
                clearancePrice
        );
    }

    private void validateBatchRequest(BatchRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Batch details are required"
            );
        }

        if (request.getProductId() == null) {
            throw new IllegalArgumentException(
                    "Product is required"
            );
        }

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero"
            );
        }

        validateCommonFields(request);
    }

    private void validateBatchRequestForUpdate(BatchRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Batch details are required"
            );
        }

        if (request.getQuantity() != null) {
            throw new IllegalArgumentException(
                    "Stock quantity cannot be changed through batch editing"
            );
        }

        validateCommonFields(request);
    }

    private void validateCommonFields(BatchRequest request) {

        if (request.getBatchNumber() == null
                || request.getBatchNumber().isBlank()) {
            throw new IllegalArgumentException(
                    "Batch number is required"
            );
        }

        if (request.getManufacturingDate() == null
                || request.getExpiryDate() == null
                || request.getReceivedDate() == null) {
            throw new IllegalArgumentException(
                    "Manufacturing, expiry, and received dates are required"
            );
        }

        if (request.getManufacturingDate()
                .isAfter(request.getExpiryDate())) {
            throw new IllegalArgumentException(
                    "Manufacturing date cannot be after expiry date"
            );
        }

        if (request.getManufacturingDate()
                .isAfter(request.getReceivedDate())) {
            throw new IllegalArgumentException(
                    "Received date cannot be before manufacturing date"
            );
        }

        if (request.getReceivedDate()
                .isAfter(request.getExpiryDate())) {
            throw new IllegalArgumentException(
                    "Received date cannot be after expiry date"
            );
        }

        if (request.getPurchasePrice() == null
                || request.getPurchasePrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Purchase price must be greater than zero"
            );
        }
    }
}