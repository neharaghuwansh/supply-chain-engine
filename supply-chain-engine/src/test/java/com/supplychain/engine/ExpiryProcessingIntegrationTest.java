package com.supplychain.engine;

import com.supplychain.engine.entity.Batch;
import com.supplychain.engine.entity.InventoryTransaction;
import com.supplychain.engine.entity.InventoryTransactionType;
import com.supplychain.engine.entity.Product;
import com.supplychain.engine.repository.BatchRepository;
import com.supplychain.engine.repository.InventoryTransactionRepository;
import com.supplychain.engine.repository.ProductRepository;
import com.supplychain.engine.service.ExpiryProcessingService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ExpiryProcessingIntegrationTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private InventoryTransactionRepository transactionRepository;

    @Autowired
    private ExpiryProcessingService expiryProcessingService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldProcessExpiredBatchesAndIgnoreFutureBatches() {

        Product product = new Product();
        product.setName("Expiry Integration Test Product");
        product.setSku("EXP-TEST-" + UUID.randomUUID());
        product.setCategory("Medicine");
        product.setUnit("Strip");
        product.setBaseSellingPrice(new BigDecimal("50.00"));

        product = productRepository.saveAndFlush(product);

        // Create an expired batch with available stock.
        Batch expiredBatch = createBatch(
                product,
                "EXP-BATCH-" + UUID.randomUUID(),
                LocalDate.now().minusDays(1),
                25
        );

        expiredBatch = batchRepository.saveAndFlush(expiredBatch);
        Long expiredBatchId = expiredBatch.getId();

        // Create a future-expiry batch.
        Batch futureBatch = createBatch(
                product,
                "FUTURE-BATCH-" + UUID.randomUUID(),
                LocalDate.now().plusDays(30),
                15
        );

        futureBatch = batchRepository.saveAndFlush(futureBatch);
        Long futureBatchId = futureBatch.getId();

        entityManager.flush();
        entityManager.clear();

        // Execute the actual expiry-processing workflow.
        expiryProcessingService.processExpiredBatches();

        entityManager.flush();
        entityManager.clear();

        // Verify that expired stock was completely written off.
        Batch updatedExpiredBatch = batchRepository.findById(expiredBatchId)
                .orElseThrow();

        assertEquals(0, updatedExpiredBatch.getQuantity());

        // Verify that future-expiry stock remains untouched.
        Batch updatedFutureBatch = batchRepository.findById(futureBatchId)
                .orElseThrow();

        assertEquals(15, updatedFutureBatch.getQuantity());

        // Verify the expiry transaction was persisted correctly.
        List<InventoryTransaction> expiryTransactions =
                transactionRepository.findAll()
                        .stream()
                        .filter(transaction ->
                                transaction.getBatch().getId().equals(expiredBatchId)
                                        && transaction.getType()
                                        == InventoryTransactionType.EXPIRED
                        )
                        .toList();

        assertEquals(1, expiryTransactions.size());

        InventoryTransaction expiryTransaction = expiryTransactions.get(0);

        assertEquals(25, expiryTransaction.getQuantity());
        assertEquals(-25, expiryTransaction.getQuantityChange());
        assertEquals("EXPIRY-PROCESS", expiryTransaction.getReference());

        // Run the workflow again to verify duplicate write-offs are prevented.
        expiryProcessingService.processExpiredBatches();

        entityManager.flush();
        entityManager.clear();

        List<InventoryTransaction> transactionsAfterSecondRun =
                transactionRepository.findAll()
                        .stream()
                        .filter(transaction ->
                                transaction.getBatch().getId().equals(expiredBatchId)
                                        && transaction.getType()
                                        == InventoryTransactionType.EXPIRED
                        )
                        .toList();

        assertEquals(1, transactionsAfterSecondRun.size());

        Batch batchAfterSecondRun = batchRepository.findById(expiredBatchId)
                .orElseThrow();

        assertEquals(0, batchAfterSecondRun.getQuantity());
    }

    private Batch createBatch(
            Product product,
            String batchNumber,
            LocalDate expiryDate,
            int quantity
    ) {
        Batch batch = new Batch();

        batch.setProduct(product);
        batch.setBatchNumber(batchNumber);
        batch.setManufacturingDate(LocalDate.now().minusDays(60));
        batch.setExpiryDate(expiryDate);
        batch.setQuantity(quantity);
        batch.setPurchasePrice(new BigDecimal("30.00"));
        batch.setReceivedDate(LocalDate.now().minusDays(30));
        batch.setArchived(false);

        return batch;
    }
}