package com.supplychain.engine;

import com.supplychain.engine.entity.Batch;
import com.supplychain.engine.entity.InventoryTransaction;
import com.supplychain.engine.entity.InventoryTransactionType;
import com.supplychain.engine.repository.BatchRepository;
import com.supplychain.engine.repository.InventoryTransactionRepository;
import com.supplychain.engine.service.InventoryTransactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryTransactionServiceTest {

    @Mock
    private InventoryTransactionRepository transactionRepository;

    @Mock
    private BatchRepository batchRepository;

    @InjectMocks
    private InventoryTransactionService inventoryTransactionService;

    @Test
    void shouldWriteOffExpiredBatchSuccessfully() {

        Batch batch = createBatch(
                1L,
                "EXP-001",
                LocalDate.now().minusDays(1),
                50,
                false
        );

        when(batchRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(batch));

        when(transactionRepository.save(any(InventoryTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InventoryTransaction result =
                inventoryTransactionService.writeOffExpiredBatch(
                        1L,
                        "EXPIRY-PROCESS"
                );

        assertNotNull(result);

        assertEquals(0, batch.getQuantity());

        assertEquals(
                InventoryTransactionType.EXPIRED,
                result.getType()
        );

        assertEquals(50, result.getQuantity());
        assertEquals(-50, result.getQuantityChange());
        assertEquals("EXPIRY-PROCESS", result.getReference());

        verify(batchRepository).findByIdForUpdate(1L);
        verify(batchRepository).save(batch);
        verify(transactionRepository).save(any(InventoryTransaction.class));
    }

    @Test
    void shouldIgnoreArchivedBatch() {

        Batch batch = createBatch(
                2L,
                "EXP-002",
                LocalDate.now().minusDays(1),
                50,
                true
        );

        when(batchRepository.findByIdForUpdate(2L))
                .thenReturn(Optional.of(batch));

        InventoryTransaction result =
                inventoryTransactionService.writeOffExpiredBatch(
                        2L,
                        "EXPIRY-PROCESS"
                );

        assertNull(result);
        assertEquals(50, batch.getQuantity());

        verify(batchRepository, never()).save(any(Batch.class));
        verify(transactionRepository, never())
                .save(any(InventoryTransaction.class));
    }

    @Test
    void shouldIgnoreBatchWithMissingExpiryDate() {

        Batch batch = createBatch(
                3L,
                "EXP-003",
                null,
                50,
                false
        );

        when(batchRepository.findByIdForUpdate(3L))
                .thenReturn(Optional.of(batch));

        InventoryTransaction result =
                inventoryTransactionService.writeOffExpiredBatch(
                        3L,
                        "EXPIRY-PROCESS"
                );

        assertNull(result);
        assertEquals(50, batch.getQuantity());

        verify(batchRepository, never()).save(any(Batch.class));
        verify(transactionRepository, never())
                .save(any(InventoryTransaction.class));
    }

    @Test
    void shouldIgnoreBatchExpiringInFuture() {

        Batch batch = createBatch(
                4L,
                "EXP-004",
                LocalDate.now().plusDays(10),
                50,
                false
        );

        when(batchRepository.findByIdForUpdate(4L))
                .thenReturn(Optional.of(batch));

        InventoryTransaction result =
                inventoryTransactionService.writeOffExpiredBatch(
                        4L,
                        "EXPIRY-PROCESS"
                );

        assertNull(result);
        assertEquals(50, batch.getQuantity());

        verify(batchRepository, never()).save(any(Batch.class));
        verify(transactionRepository, never())
                .save(any(InventoryTransaction.class));
    }

    @Test
    void shouldIgnoreBatchWithZeroQuantity() {

        Batch batch = createBatch(
                5L,
                "EXP-005",
                LocalDate.now().minusDays(1),
                0,
                false
        );

        when(batchRepository.findByIdForUpdate(5L))
                .thenReturn(Optional.of(batch));

        InventoryTransaction result =
                inventoryTransactionService.writeOffExpiredBatch(
                        5L,
                        "EXPIRY-PROCESS"
                );

        assertNull(result);
        assertEquals(0, batch.getQuantity());

        verify(batchRepository, never()).save(any(Batch.class));
        verify(transactionRepository, never())
                .save(any(InventoryTransaction.class));
    }

    @Test
    void shouldNotWriteOffSameBatchTwice() {

        Batch batch = createBatch(
                6L,
                "EXP-006",
                LocalDate.now().minusDays(1),
                30,
                false
        );

        when(batchRepository.findByIdForUpdate(6L))
                .thenReturn(Optional.of(batch));

        when(transactionRepository.save(any(InventoryTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InventoryTransaction firstResult =
                inventoryTransactionService.writeOffExpiredBatch(
                        6L,
                        "EXPIRY-PROCESS"
                );

        InventoryTransaction secondResult =
                inventoryTransactionService.writeOffExpiredBatch(
                        6L,
                        "EXPIRY-PROCESS"
                );

        assertNotNull(firstResult);
        assertNull(secondResult);

        assertEquals(0, batch.getQuantity());

        verify(transactionRepository, org.mockito.Mockito.times(1))
                .save(any(InventoryTransaction.class));
    }

    @Test
    void shouldWriteOffBatchExpiringToday() {

        Batch batch = createBatch(
                7L,
                "EXP-007",
                LocalDate.now(),
                20,
                false
        );

        when(batchRepository.findByIdForUpdate(7L))
                .thenReturn(Optional.of(batch));

        when(transactionRepository.save(any(InventoryTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InventoryTransaction result =
                inventoryTransactionService.writeOffExpiredBatch(
                        7L,
                        "EXPIRY-PROCESS"
                );

        assertNotNull(result);
        assertEquals(0, batch.getQuantity());
        assertEquals(20, result.getQuantity());
        assertEquals(-20, result.getQuantityChange());
    }

    private Batch createBatch(
            Long id,
            String batchNumber,
            LocalDate expiryDate,
            int quantity,
            boolean archived) {

        Batch batch = new Batch();

        batch.setId(id);
        batch.setBatchNumber(batchNumber);
        batch.setExpiryDate(expiryDate);
        batch.setQuantity(quantity);
        batch.setArchived(archived);

        return batch;
    }
}