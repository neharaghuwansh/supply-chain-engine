package com.supplychain.engine;

import com.supplychain.engine.entity.Batch;
import com.supplychain.engine.repository.BatchRepository;
import com.supplychain.engine.service.ExpiryProcessingService;
import com.supplychain.engine.service.InventoryTransactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpiryProcessingServiceTest {

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private InventoryTransactionService inventoryTransactionService;

    @InjectMocks
    private ExpiryProcessingService expiryProcessingService;

    @Test
    void shouldDelegateEveryBatchToInventoryTransactionService() {

        Batch expiredBatch = createBatch(1L);
        Batch futureBatch = createBatch(2L);

        when(batchRepository.findAll())
                .thenReturn(List.of(expiredBatch, futureBatch));

        expiryProcessingService.processExpiredBatches();

        verify(inventoryTransactionService)
                .writeOffExpiredBatch(1L, "EXPIRY-PROCESS");

        verify(inventoryTransactionService)
                .writeOffExpiredBatch(2L, "EXPIRY-PROCESS");
    }

    private Batch createBatch(Long id) {

        Batch batch = new Batch();
        batch.setId(id);

        return batch;
    }
}