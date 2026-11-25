package com.supplychain.engine.service;
import java.time.ZoneId;
import com.supplychain.engine.entity.Batch;
import com.supplychain.engine.entity.InventoryTransaction;
import com.supplychain.engine.entity.InventoryTransactionType;
import com.supplychain.engine.entity.SaleItem;
import com.supplychain.engine.repository.BatchRepository;
import com.supplychain.engine.repository.InventoryTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class InventoryTransactionService {

    private final InventoryTransactionRepository transactionRepository;
    private final BatchRepository batchRepository;

    public InventoryTransactionService(
            InventoryTransactionRepository transactionRepository,
            BatchRepository batchRepository) {

        this.transactionRepository = transactionRepository;
        this.batchRepository = batchRepository;
    }

    @Transactional
    public InventoryTransaction recordTransaction(
            Long batchId,
            InventoryTransactionType type,
            int quantity,
            String reference) {

        if (type == null) {
            throw new IllegalArgumentException(
                    "Transaction type is required"
            );
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Transaction quantity must be greater than zero"
            );
        }

        if (type == InventoryTransactionType.ADJUSTMENT) {
            throw new IllegalArgumentException(
                    "Use a dedicated adjustment operation for stock adjustments"
            );
        }

        if (type == InventoryTransactionType.RETURN) {
            throw new IllegalArgumentException(
                    "Use recordReturnTransaction for customer returns"
            );
        }

        Batch batch = batchRepository.findByIdForUpdate(batchId)
                .orElseThrow(() ->
                        new RuntimeException("Batch not found"));

        validateBatchNotArchived(batch);

        int quantityChange;

        switch (type) {
            case PURCHASE -> {
                increaseStock(batch, quantity);
                quantityChange = quantity;
            }

            case SALE -> {
                validateBatchCanBeSold(batch);
                decreaseStock(batch, quantity);
                quantityChange = -quantity;
            }

            case DAMAGE -> {
                decreaseStock(batch, quantity);
                quantityChange = -quantity;
            }

            case EXPIRED -> {
                validateBatchExpiryForWriteOff(batch);
                decreaseStock(batch, quantity);
                quantityChange = -quantity;
            }

            default -> throw new IllegalArgumentException(
                    "Unsupported transaction type: " + type
            );
        }

        batchRepository.save(batch);

        return saveTransaction(
                batch,
                type,
                quantity,
                quantityChange,
                reference,
                null
        );
    }

    @Transactional
    public InventoryTransaction writeOffExpiredBatch(
            Long batchId,
            String reference) {

        Batch batch = batchRepository.findByIdForUpdate(batchId)
                .orElseThrow(() ->
                        new RuntimeException("Batch not found"));

        // Archived batches must not be processed.
        if (batch.isArchived()) {
            return null;
        }

        // Batches without expiry dates cannot be processed.
        if (batch.getExpiryDate() == null) {
            return null;
        }

        // Do not process batches that have not expired.
        if (batch.getExpiryDate().isAfter(LocalDate.now(ZoneId.of("Asia/Kolkata")))) {
            return null;
        }

        // Ignore batches with no remaining stock.
        if (batch.getQuantity() <= 0) {
            return null;
        }

        // Read the latest quantity while holding the database lock.
        int expiredQuantity = batch.getQuantity();

        validateBatchExpiryForWriteOff(batch);
        decreaseStock(batch, expiredQuantity);

        batchRepository.save(batch);

        return saveTransaction(
                batch,
                InventoryTransactionType.EXPIRED,
                expiredQuantity,
                -expiredQuantity,
                reference,
                null
        );
    }

    @Transactional
    public InventoryTransaction recordSaleTransaction(
            Long batchId,
            int quantity,
            String reference,
            SaleItem saleItem) {

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Transaction quantity must be greater than zero"
            );
        }

        Batch batch = batchRepository.findByIdForUpdate(batchId)
                .orElseThrow(() ->
                        new RuntimeException("Batch not found"));

        validateBatchNotArchived(batch);
        validateBatchCanBeSold(batch);

        decreaseStock(batch, quantity);
        batchRepository.save(batch);

        return saveTransaction(
                batch,
                InventoryTransactionType.SALE,
                quantity,
                -quantity,
                reference,
                saleItem
        );
    }

    @Transactional
    public InventoryTransaction recordReturnTransaction(
            Long batchId,
            int quantity,
            String reference,
            SaleItem saleItem) {

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Transaction quantity must be greater than zero"
            );
        }

        Batch batch = batchRepository.findByIdForUpdate(batchId)
                .orElseThrow(() ->
                        new RuntimeException("Batch not found"));

        validateBatchNotArchived(batch);

        boolean expired = batch.getExpiryDate() == null
                || !batch.getExpiryDate().isAfter(LocalDate.now(ZoneId.of("Asia/Kolkata")));

        if (!expired) {
            increaseStock(batch, quantity);
            batchRepository.save(batch);
        }

        return saveTransaction(
                batch,
                InventoryTransactionType.RETURN,
                quantity,
                expired ? 0 : quantity,
                expired
                        ? reference + "-EXPIRED-NOT-RESTOCKED"
                        : reference,
                saleItem
        );
    }

    @Transactional
    public InventoryTransaction adjustStock(
            Long batchId,
            int adjustmentQuantity,
            String reference) {

        if (adjustmentQuantity == 0) {
            throw new IllegalArgumentException(
                    "Adjustment quantity cannot be zero"
            );
        }

        Batch batch = batchRepository.findByIdForUpdate(batchId)
                .orElseThrow(() ->
                        new RuntimeException("Batch not found"));

        validateBatchNotArchived(batch);

        int newQuantity;

        try {
            newQuantity = Math.addExact(
                    batch.getQuantity(),
                    adjustmentQuantity
            );
        } catch (ArithmeticException exception) {
            throw new IllegalStateException(
                    "Adjusted stock quantity exceeds the supported maximum",
                    exception
            );
        }

        if (newQuantity < 0) {
            throw new IllegalStateException(
                    "Adjustment would make stock negative"
            );
        }

        batch.setQuantity(newQuantity);
        batchRepository.save(batch);

        return saveTransaction(
                batch,
                InventoryTransactionType.ADJUSTMENT,
                Math.abs(adjustmentQuantity),
                adjustmentQuantity,
                reference,
                null
        );
    }

    @Transactional
    public InventoryTransaction deductStock(
            Long batchId,
            int quantity,
            String reference) {

        return recordSaleTransaction(
                batchId,
                quantity,
                reference,
                null
        );
    }

    private InventoryTransaction saveTransaction(
            Batch batch,
            InventoryTransactionType type,
            int quantity,
            int quantityChange,
            String reference,
            SaleItem saleItem) {

        InventoryTransaction transaction = new InventoryTransaction();

        transaction.setBatch(batch);
        transaction.setSaleItem(saleItem);
        transaction.setType(type);
        transaction.setQuantity(quantity);
        transaction.setQuantityChange(quantityChange);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setReference(reference);

        return transactionRepository.save(transaction);
    }

    private void validateBatchNotArchived(Batch batch) {

        if (batch.isArchived()) {
            throw new IllegalStateException(
                    "Cannot modify stock for archived batch: "
                            + batch.getBatchNumber()
            );
        }
    }

    private void validateBatchCanBeSold(Batch batch) {

        if (batch.getExpiryDate() == null) {
            throw new IllegalStateException(
                    "Batch expiry date is missing"
            );
        }

        if (!batch.getExpiryDate().isAfter(LocalDate.now(ZoneId.of("Asia/Kolkata")))) {
            throw new IllegalStateException(
                    "Cannot sell expired batch: "
                            + batch.getBatchNumber()
            );
        }
    }

    private void validateBatchExpiryForWriteOff(Batch batch) {

        if (batch.getExpiryDate() == null) {
            throw new IllegalStateException(
                    "Batch expiry date is missing"
            );
        }

        if (batch.getExpiryDate().isAfter(LocalDate.now(ZoneId.of("Asia/Kolkata")))) {
            throw new IllegalStateException(
                    "Cannot write off stock as expired before its expiry date: "
                            + batch.getBatchNumber()
            );
        }
    }

    private void increaseStock(Batch batch, int quantity) {

        try {
            batch.setQuantity(
                    Math.addExact(batch.getQuantity(), quantity)
            );
        } catch (ArithmeticException exception) {
            throw new IllegalStateException(
                    "Stock quantity exceeds the supported maximum",
                    exception
            );
        }
    }

    private void decreaseStock(Batch batch, int quantity) {

        if (quantity > batch.getQuantity()) {
            throw new IllegalStateException(
                    "Insufficient stock in batch"
            );
        }

        batch.setQuantity(batch.getQuantity() - quantity);
    }
}