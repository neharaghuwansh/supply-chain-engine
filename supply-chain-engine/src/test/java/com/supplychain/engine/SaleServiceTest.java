package com.supplychain.engine;

import com.supplychain.engine.dto.ReturnRequest;
import com.supplychain.engine.dto.SaleRequest;
import com.supplychain.engine.entity.Batch;
import com.supplychain.engine.entity.ExpiryRisk;
import com.supplychain.engine.entity.InventoryTransaction;
import com.supplychain.engine.entity.InventoryTransactionType;
import com.supplychain.engine.entity.Product;
import com.supplychain.engine.entity.Sale;
import com.supplychain.engine.entity.SaleItem;
import com.supplychain.engine.repository.BatchRepository;
import com.supplychain.engine.repository.InventoryTransactionRepository;
import com.supplychain.engine.repository.ProductRepository;
import com.supplychain.engine.repository.SaleItemRepository;
import com.supplychain.engine.repository.SaleRepository;
import com.supplychain.engine.service.ClearancePricingService;
import com.supplychain.engine.service.ExpiryRiskService;
import com.supplychain.engine.service.FefoService;
import com.supplychain.engine.service.InventoryTransactionService;
import com.supplychain.engine.service.SaleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private SaleItemRepository saleItemRepository;

    @Mock
    private InventoryTransactionRepository transactionRepository;

    @Mock
    private FefoService fefoService;

    @Mock
    private InventoryTransactionService inventoryTransactionService;

    @Mock
    private ClearancePricingService clearancePricingService;

    @Mock
    private ExpiryRiskService expiryRiskService;

    @InjectMocks
    private SaleService saleService;

    // ==========================================
    // SALE TESTS
    // ==========================================

    @Test
    void shouldCreateSaleUsingFefoAllocation() {

        Product product = createProduct();

        Batch batch = createBatch(
                1L,
                "PCM-A01",
                LocalDate.now().plusDays(10),
                100,
                product
        );

        SaleRequest request = createRequest(1L, 20);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(fefoService.allocateStock(1L, 20))
                .thenReturn(List.of(
                        new FefoService.BatchAllocation(
                                1L,
                                "PCM-A01",
                                20
                        )
                ));

        when(batchRepository.findById(1L))
                .thenReturn(Optional.of(batch));

        when(expiryRiskService.calculateRisk(batch.getExpiryDate()))
                .thenReturn(ExpiryRisk.HIGH);

        when(clearancePricingService.calculateClearancePrice(
                product.getBaseSellingPrice(),
                ExpiryRisk.HIGH
        )).thenReturn(new BigDecimal("20.00"));

        when(saleRepository.saveAndFlush(any(Sale.class)))
                .thenAnswer(invocation -> {
                    Sale sale = invocation.getArgument(0);
                    sale.setId(100L);
                    return sale;
                });

        Sale result = saleService.createSale(request);

        assertEquals(100L, result.getId());
        assertEquals(new BigDecimal("400.00"), result.getTotalAmount());
        assertEquals(1, result.getItems().size());
        assertEquals(20, result.getItems().get(0).getQuantity());

        assertEquals(
                new BigDecimal("20.00"),
                result.getItems().get(0).getSellingPrice()
        );

        verify(fefoService).allocateStock(1L, 20);

        verify(inventoryTransactionService)
                .recordSaleTransaction(
                        1L,
                        20,
                        "SALE-100",
                        result.getItems().get(0)
                );
    }

    @Test
    void shouldRejectExpiredBatchAllocatedByFefo() {

        Product product = createProduct();

        Batch expiredBatch = createBatch(
                1L,
                "PCM-A01",
                LocalDate.now().minusDays(3),
                50,
                product
        );

        SaleRequest request = createRequest(1L, 1);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(fefoService.allocateStock(1L, 1))
                .thenReturn(List.of(
                        new FefoService.BatchAllocation(
                                1L,
                                "PCM-A01",
                                1
                        )
                ));

        when(batchRepository.findById(1L))
                .thenReturn(Optional.of(expiredBatch));

        assertThrows(
                RuntimeException.class,
                () -> saleService.createSale(request)
        );

        verify(saleRepository, never())
                .saveAndFlush(any(Sale.class));

        verify(inventoryTransactionService, never())
                .recordSaleTransaction(
                        any(Long.class),
                        any(Integer.class),
                        any(String.class),
                        any()
                );
    }

    // ==========================================
    // CUSTOMER RETURN TESTS
    // ==========================================

    @Test
    void shouldProcessPartialReturnSuccessfully() {

        SaleItem saleItem = createSaleItem(5, 50L);

        ReturnRequest request =
                createReturnRequest(100L, 50L, 2);

        InventoryTransaction transaction =
                new InventoryTransaction();

        when(saleItemRepository.findByIdForUpdate(50L))
                .thenReturn(Optional.of(saleItem));

        when(transactionRepository.sumQuantityBySaleItemIdAndType(
                50L,
                InventoryTransactionType.RETURN
        )).thenReturn(0);

        when(inventoryTransactionService.recordReturnTransaction(
                7L,
                2,
                "CUSTOMER-RETURN-SALE-100",
                saleItem
        )).thenReturn(transaction);

        InventoryTransaction result =
                saleService.processReturn(request);

        assertEquals(transaction, result);

        verify(inventoryTransactionService)
                .recordReturnTransaction(
                        7L,
                        2,
                        "CUSTOMER-RETURN-SALE-100",
                        saleItem
                );
    }

    @Test
    void shouldProcessFullReturnSuccessfully() {

        SaleItem saleItem = createSaleItem(5, 50L);

        ReturnRequest request =
                createReturnRequest(100L, 50L, 5);

        when(saleItemRepository.findByIdForUpdate(50L))
                .thenReturn(Optional.of(saleItem));

        when(transactionRepository.sumQuantityBySaleItemIdAndType(
                50L,
                InventoryTransactionType.RETURN
        )).thenReturn(0);

        saleService.processReturn(request);

        verify(inventoryTransactionService)
                .recordReturnTransaction(
                        7L,
                        5,
                        "CUSTOMER-RETURN-SALE-100",
                        saleItem
                );
    }

    @Test
    void shouldRejectReturnExceedingRemainingQuantity() {

        SaleItem saleItem = createSaleItem(5, 50L);

        ReturnRequest request =
                createReturnRequest(100L, 50L, 4);

        when(saleItemRepository.findByIdForUpdate(50L))
                .thenReturn(Optional.of(saleItem));

        when(transactionRepository.sumQuantityBySaleItemIdAndType(
                50L,
                InventoryTransactionType.RETURN
        )).thenReturn(2);

        assertThrows(
                IllegalArgumentException.class,
                () -> saleService.processReturn(request)
        );

        verify(inventoryTransactionService, never())
                .recordReturnTransaction(
                        any(Long.class),
                        any(Integer.class),
                        any(String.class),
                        any()
                );
    }

    @Test
    void shouldRejectReturnWhenAllQuantityAlreadyReturned() {

        SaleItem saleItem = createSaleItem(5, 50L);

        ReturnRequest request =
                createReturnRequest(100L, 50L, 1);

        when(saleItemRepository.findByIdForUpdate(50L))
                .thenReturn(Optional.of(saleItem));

        when(transactionRepository.sumQuantityBySaleItemIdAndType(
                50L,
                InventoryTransactionType.RETURN
        )).thenReturn(5);

        assertThrows(
                IllegalArgumentException.class,
                () -> saleService.processReturn(request)
        );

        verify(inventoryTransactionService, never())
                .recordReturnTransaction(
                        any(Long.class),
                        any(Integer.class),
                        any(String.class),
                        any()
                );
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private Product createProduct() {

        Product product = new Product();

        product.setId(1L);
        product.setName("Paracetamol 500mg");
        product.setBaseSellingPrice(new BigDecimal("25.00"));
        product.setActive(true);

        return product;
    }

    private Batch createBatch(
            Long id,
            String batchNumber,
            LocalDate expiryDate,
            int quantity,
            Product product
    ) {

        Batch batch = new Batch();

        batch.setId(id);
        batch.setBatchNumber(batchNumber);
        batch.setExpiryDate(expiryDate);
        batch.setQuantity(quantity);
        batch.setProduct(product);

        return batch;
    }

    private SaleItem createSaleItem(
            int quantity,
            Long saleItemId
    ) {

        Sale sale = new Sale();
        sale.setId(100L);

        Product product = createProduct();

        Batch batch = createBatch(
                7L,
                "PCM-A01",
                LocalDate.now().plusDays(20),
                10,
                product
        );

        SaleItem saleItem = new SaleItem();

        saleItem.setId(saleItemId);
        saleItem.setSale(sale);
        saleItem.setProduct(product);
        saleItem.setBatch(batch);
        saleItem.setQuantity(quantity);

        return saleItem;
    }

    private SaleRequest createRequest(
            Long productId,
            int quantity
    ) {

        SaleRequest.SaleItemRequest item =
                new SaleRequest.SaleItemRequest();

        item.setProductId(productId);
        item.setQuantity(quantity);

        SaleRequest request = new SaleRequest();

        request.setIdempotencyKey(UUID.randomUUID().toString());
        request.setItems(List.of(item));

        return request;
    }

    private ReturnRequest createReturnRequest(
            Long saleId,
            Long saleItemId,
            int quantity
    ) {

        ReturnRequest request = new ReturnRequest();

        request.setSaleId(saleId);
        request.setSaleItemId(saleItemId);
        request.setQuantity(quantity);

        return request;
    }
}