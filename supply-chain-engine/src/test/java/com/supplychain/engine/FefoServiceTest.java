package com.supplychain.engine;

import com.supplychain.engine.entity.Batch;
import com.supplychain.engine.repository.BatchRepository;
import com.supplychain.engine.service.FefoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FefoServiceTest {

    @Mock
    private BatchRepository batchRepository;

    @InjectMocks
    private FefoService fefoService;

    @Test
    void shouldAllocateStockUsingFefo() {

        Batch batchA = createBatch(
                1L, "PCM-A01",
                LocalDate.now().plusDays(10), 100
        );

        Batch batchB = createBatch(
                2L, "PCM-B01",
                LocalDate.now().plusDays(70), 150
        );

        when(batchRepository.findAvailableBatchesForUpdate(1L))
                .thenReturn(List.of(batchA, batchB));

        List<FefoService.BatchAllocation> result =
                fefoService.allocateStock(1L, 120);

        assertEquals(2, result.size());

        assertEquals(1L, result.get(0).batchId());
        assertEquals("PCM-A01", result.get(0).batchNumber());
        assertEquals(100, result.get(0).quantity());

        assertEquals(2L, result.get(1).batchId());
        assertEquals("PCM-B01", result.get(1).batchNumber());
        assertEquals(20, result.get(1).quantity());

        verify(batchRepository).findAvailableBatchesForUpdate(1L);
    }

    @Test
    void shouldSkipExpiredBatches() {

        Batch expiredBatch = createBatch(
                1L, "EXPIRED-BATCH",
                LocalDate.now().minusDays(1), 100
        );

        Batch validBatch = createBatch(
                2L, "VALID-BATCH",
                LocalDate.now().plusDays(10), 50
        );

        when(batchRepository.findAvailableBatchesForUpdate(1L))
                .thenReturn(List.of(expiredBatch, validBatch));

        List<FefoService.BatchAllocation> result =
                fefoService.allocateStock(1L, 30);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).batchId());
        assertEquals(30, result.get(0).quantity());
    }

    @Test
    void shouldSkipBatchesExpiringToday() {

        Batch expiringToday = createBatch(
                1L, "EXPIRING-TODAY",
                LocalDate.now(), 100
        );

        Batch validBatch = createBatch(
                2L, "VALID-BATCH",
                LocalDate.now().plusDays(1), 50
        );

        when(batchRepository.findAvailableBatchesForUpdate(1L))
                .thenReturn(List.of(expiringToday, validBatch));

        List<FefoService.BatchAllocation> result =
                fefoService.allocateStock(1L, 30);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).batchId());
        assertEquals("VALID-BATCH", result.get(0).batchNumber());
        assertEquals(30, result.get(0).quantity());
    }
    @Test
    void shouldSkipBatchesWithNullExpiryDate() {

        Batch batchWithoutExpiry = createBatch(
                1L, "NO-EXPIRY",
                null, 100
        );

        Batch validBatch = createBatch(
                2L, "VALID-BATCH",
                LocalDate.now().plusDays(10), 50
        );

        when(batchRepository.findAvailableBatchesForUpdate(1L))
                .thenReturn(List.of(batchWithoutExpiry, validBatch));

        List<FefoService.BatchAllocation> result =
                fefoService.allocateStock(1L, 30);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).batchId());
        assertEquals(30, result.get(0).quantity());
    }
    @Test
    void shouldRejectInsufficientSellableStock() {

        Batch batch = createBatch(
                1L, "PCM-A01",
                LocalDate.now().plusDays(10), 5
        );

        when(batchRepository.findAvailableBatchesForUpdate(1L))
                .thenReturn(List.of(batch));

        assertThrows(
                IllegalStateException.class,
                () -> fefoService.allocateStock(1L, 10)
        );
    }

    @Test
    void shouldRejectZeroRequestedQuantity() {

        assertThrows(
                IllegalArgumentException.class,
                () -> fefoService.allocateStock(1L, 0)
        );
    }

    private Batch createBatch(
            Long id,
            String batchNumber,
            LocalDate expiryDate,
            int quantity) {

        Batch batch = new Batch();
        batch.setId(id);
        batch.setBatchNumber(batchNumber);
        batch.setExpiryDate(expiryDate);
        batch.setQuantity(quantity);

        return batch;
    }
}