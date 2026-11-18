package com.supplychain.engine.service;

import com.supplychain.engine.entity.Batch;
import com.supplychain.engine.repository.BatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class FefoService {

    private final BatchRepository batchRepository;

    public FefoService(BatchRepository batchRepository) {
        this.batchRepository = batchRepository;
    }

    @Transactional
    public List<BatchAllocation> allocateStock(
            Long productId,
            int requestedQuantity) {

        if (productId == null) {
            throw new IllegalArgumentException(
                    "Product ID is required"
            );
        }

        if (requestedQuantity <= 0) {
            throw new IllegalArgumentException(
                    "Requested quantity must be greater than zero"
            );
        }

        // Retrieve and lock batches in FEFO order.
        List<Batch> batches =
                batchRepository.findAvailableBatchesForUpdate(productId);

        List<BatchAllocation> allocations = new ArrayList<>();
        int remainingQuantity = requestedQuantity;
        LocalDate today = LocalDate.now();

        for (Batch batch : batches) {

            // Never allocate stock from expired batches or batches
            // expiring today.
            if (batch.getExpiryDate() == null
                    || !batch.getExpiryDate().isAfter(today)) {
                continue;
            }

            if (remainingQuantity == 0) {
                break;
            }

            int availableQuantity = batch.getQuantity();

            if (availableQuantity <= 0) {
                continue;
            }

            int quantityToAllocate =
                    Math.min(availableQuantity, remainingQuantity);

            allocations.add(
                    new BatchAllocation(
                            batch.getId(),
                            batch.getBatchNumber(),
                            quantityToAllocate
                    )
            );

            remainingQuantity -= quantityToAllocate;
        }

        if (remainingQuantity > 0) {
            throw new IllegalStateException(
                    "Insufficient sellable stock for product ID: "
                            + productId
            );
        }

        return allocations;
    }

    public record BatchAllocation(
            Long batchId,
            String batchNumber,
            int quantity
    ) {
    }
}