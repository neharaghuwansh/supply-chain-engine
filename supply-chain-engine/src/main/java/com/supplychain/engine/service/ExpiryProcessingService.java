package com.supplychain.engine.service;

import com.supplychain.engine.entity.Batch;
import com.supplychain.engine.repository.BatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ExpiryProcessingService {

    private final BatchRepository batchRepository;
    private final InventoryTransactionService inventoryTransactionService;

    public ExpiryProcessingService(
            BatchRepository batchRepository,
            InventoryTransactionService inventoryTransactionService) {

        this.batchRepository = batchRepository;
        this.inventoryTransactionService = inventoryTransactionService;
    }

    @Transactional
    public void processExpiredBatches() {

        List<Batch> batches = batchRepository.findAll();

        for (Batch batch : batches) {

            inventoryTransactionService.writeOffExpiredBatch(
                    batch.getId(),
                    "EXPIRY-PROCESS"
            );
        }
    }
}