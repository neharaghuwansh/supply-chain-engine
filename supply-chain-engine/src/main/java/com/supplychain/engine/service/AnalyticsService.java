package com.supplychain.engine.service;

import com.supplychain.engine.dto.AnalyticsReportResponse;
import com.supplychain.engine.dto.AnalyticsResponse;
import com.supplychain.engine.entity.Batch;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Set;

@Service
public class AnalyticsService {

    private static final int EXPIRY_RISK_DAYS = 30;

    private final ProductRepository productRepository;
    private final BatchRepository batchRepository;
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final InventoryTransactionRepository transactionRepository;

    public AnalyticsService(
            ProductRepository productRepository,
            BatchRepository batchRepository,
            SaleRepository saleRepository,
            SaleItemRepository saleItemRepository,
            InventoryTransactionRepository transactionRepository) {

        this.productRepository = productRepository;
        this.batchRepository = batchRepository;
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse getDashboardAnalytics() {

        List<Product> products = productRepository.findAll();
        List<Batch> batches = batchRepository.findAll();
        List<Sale> sales = saleRepository.findAll();

        LocalDate today = LocalDate.now();

        long totalInventoryQuantity = 0;
        long expiredBatches = 0;
        long nearExpiryBatches = 0;

        BigDecimal totalInventoryPurchaseValue = BigDecimal.ZERO;

        for (Batch batch : batches) {

            int quantity = batch.getQuantity() == null
                    ? 0
                    : batch.getQuantity();

            totalInventoryQuantity += quantity;

            if (batch.getPurchasePrice() != null) {
                totalInventoryPurchaseValue =
                        totalInventoryPurchaseValue.add(
                                batch.getPurchasePrice()
                                        .multiply(BigDecimal.valueOf(quantity))
                        );
            }

            if (batch.getExpiryDate() != null) {

                if (batch.getExpiryDate().isBefore(today)) {
                    expiredBatches++;
                } else {
                    long daysUntilExpiry =
                            ChronoUnit.DAYS.between(
                                    today,
                                    batch.getExpiryDate()
                            );

                    if (daysUntilExpiry <= EXPIRY_RISK_DAYS) {
                        nearExpiryBatches++;
                    }
                }
            }
        }

        long lowStockProducts = 0;

        for (Product product : products) {

            int sellableQuantity = 0;

            for (Batch batch : batches) {

                if (batch.getProduct() != null
                        && batch.getProduct().getId().equals(product.getId())
                        && batch.getQuantity() != null
                        && batch.getQuantity() > 0
                        && batch.getExpiryDate() != null
                        && batch.getExpiryDate().isAfter(today)
                        && !batch.isArchived()) {

                    sellableQuantity += batch.getQuantity();
                }
            }

            if (sellableQuantity <= product.getReorderThreshold()) {
                lowStockProducts++;
            }
        }

        BigDecimal grossSalesRevenue = BigDecimal.ZERO;

        for (Sale sale : sales) {
            if (sale.getTotalAmount() != null) {
                grossSalesRevenue =
                        grossSalesRevenue.add(sale.getTotalAmount());
            }
        }

        BigDecimal totalRefundValue = calculateReturnValue(
                transactionRepository.findAll()
        );

        BigDecimal netSalesRevenue = grossSalesRevenue
                .subtract(totalRefundValue)
                .max(BigDecimal.ZERO);

        AnalyticsResponse response = new AnalyticsResponse();

        response.setTotalProducts(products.size());
        response.setTotalBatches(batches.size());
        response.setTotalInventoryQuantity(totalInventoryQuantity);
        response.setExpiredBatches(expiredBatches);
        response.setNearExpiryBatches(nearExpiryBatches);
        response.setLowStockProducts(lowStockProducts);

        // This remains the number of original sales, not the number
        // of sales remaining after returns.
        response.setTotalSales(sales.size());

        response.setTotalSalesRevenue(netSalesRevenue);
        response.setTotalInventoryPurchaseValue(
                totalInventoryPurchaseValue
        );

        return response;
    }

    @Transactional(readOnly = true)
    public AnalyticsReportResponse getReports(
            LocalDate startDate,
            LocalDate endDate) {

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "Start date and end date are required"
            );
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "End date cannot be before start date"
            );
        }

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime endExclusive = endDate.plusDays(1).atStartOfDay();

        AnalyticsReportResponse response =
                new AnalyticsReportResponse();

        response.setStartDate(startDate);
        response.setEndDate(endDate);

        response.setSalesTrend(
                calculateSalesTrend(start, endExclusive)
        );

        response.setTopSellingProducts(
                calculateTopSellingProducts(start, endExclusive)
        );

        response.setInventoryMovement(
                calculateInventoryMovement(start, endExclusive)
        );

        response.setExpiryRisk(calculateExpiryRisk());

        return response;
    }

    private BigDecimal calculateReturnValue(
            List<InventoryTransaction> transactions) {

        BigDecimal totalReturnValue = BigDecimal.ZERO;

        for (InventoryTransaction transaction : transactions) {

            if (transaction.getType() != InventoryTransactionType.RETURN) {
                continue;
            }

            SaleItem item = transaction.getSaleItem();

            if (item == null
                    || item.getSellingPrice() == null
                    || transaction.getQuantity() == null) {
                continue;
            }

            BigDecimal returnValue = item.getSellingPrice()
                    .multiply(
                            BigDecimal.valueOf(transaction.getQuantity())
                    );

            totalReturnValue = totalReturnValue.add(returnValue);
        }

        return totalReturnValue;
    }

    private List<AnalyticsReportResponse.SalesTrendPoint>
    calculateSalesTrend(
            LocalDateTime start,
            LocalDateTime endExclusive) {

        List<Sale> sales =
                saleRepository
                        .findBySaleDateGreaterThanEqualAndSaleDateLessThan(
                                start,
                                endExclusive
                        );

        List<InventoryTransaction> transactions =
                transactionRepository
                        .findByTransactionDateGreaterThanEqualAndTransactionDateLessThan(
                                start,
                                endExclusive
                        );

        Map<LocalDate, BigDecimal> grossRevenueByDate = new TreeMap<>();
        Map<LocalDate, BigDecimal> refundByDate = new TreeMap<>();

        Map<LocalDate, Long> salesCountByDate = new TreeMap<>();
        Map<LocalDate, Long> returnsCountByDate = new TreeMap<>();

        // Calculate gross sales revenue and number of sales per day.
        for (Sale sale : sales) {

            if (sale.getSaleDate() == null) {
                continue;
            }

            LocalDate date = sale.getSaleDate().toLocalDate();

            BigDecimal amount = sale.getTotalAmount() == null
                    ? BigDecimal.ZERO
                    : sale.getTotalAmount();

            grossRevenueByDate.merge(
                    date,
                    amount,
                    BigDecimal::add
            );

            salesCountByDate.merge(date, 1L, Long::sum);
        }

        // Calculate refunds and number of returns per day.
        for (InventoryTransaction transaction : transactions) {

            if (transaction.getType() != InventoryTransactionType.RETURN) {
                continue;
            }

            SaleItem item = transaction.getSaleItem();

            if (item == null
                    || item.getSellingPrice() == null
                    || transaction.getQuantity() == null
                    || transaction.getTransactionDate() == null) {
                continue;
            }

            LocalDate returnDate =
                    transaction.getTransactionDate().toLocalDate();

            BigDecimal refundValue = item.getSellingPrice()
                    .multiply(
                            BigDecimal.valueOf(transaction.getQuantity())
                    );

            refundByDate.merge(
                    returnDate,
                    refundValue,
                    BigDecimal::add
            );

            returnsCountByDate.merge(returnDate, 1L, Long::sum);
        }

        // Include dates that have sales, returns, or both.
        Set<LocalDate> allDates = new TreeSet<>();
        allDates.addAll(grossRevenueByDate.keySet());
        allDates.addAll(refundByDate.keySet());

        List<AnalyticsReportResponse.SalesTrendPoint> result =
                new ArrayList<>();

        for (LocalDate date : allDates) {

            BigDecimal grossRevenue = grossRevenueByDate.getOrDefault(
                    date,
                    BigDecimal.ZERO
            );

            BigDecimal refundAmount = refundByDate.getOrDefault(
                    date,
                    BigDecimal.ZERO
            );

            BigDecimal netRevenue = grossRevenue.subtract(refundAmount);

            long numberOfSales = salesCountByDate.getOrDefault(date, 0L);

            long numberOfReturns = returnsCountByDate.getOrDefault(date, 0L);

            result.add(
                    new AnalyticsReportResponse.SalesTrendPoint(
                            date,
                            grossRevenue,
                            refundAmount,
                            netRevenue,
                            numberOfSales,
                            numberOfReturns
                    )
            );
        }

        return result;
    }

    private List<AnalyticsReportResponse.TopSellingProduct>
    calculateTopSellingProducts(
            LocalDateTime start,
            LocalDateTime endExclusive) {

        List<SaleItem> saleItems =
                saleItemRepository.findSaleItemsForReport(
                        start,
                        endExclusive
                );

        Map<Long, ProductSalesAccumulator> totals =
                new LinkedHashMap<>();

        for (SaleItem item : saleItems) {

            if (item.getProduct() == null) {
                continue;
            }

            Product product = item.getProduct();

            ProductSalesAccumulator accumulator =
                    totals.computeIfAbsent(
                            product.getId(),
                            ignored -> new ProductSalesAccumulator(
                                    product.getId(),
                                    product.getName()
                            )
                    );

            long quantity = item.getQuantity() == null
                    ? 0L
                    : item.getQuantity();

            BigDecimal price = item.getSellingPrice() == null
                    ? BigDecimal.ZERO
                    : item.getSellingPrice();

            accumulator.quantitySold += quantity;

            accumulator.revenue =
                    accumulator.revenue.add(
                            price.multiply(BigDecimal.valueOf(quantity))
                    );
        }

        List<AnalyticsReportResponse.TopSellingProduct> result =
                new ArrayList<>();

        for (ProductSalesAccumulator accumulator : totals.values()) {

            result.add(
                    new AnalyticsReportResponse.TopSellingProduct(
                            accumulator.productId,
                            accumulator.productName,
                            accumulator.quantitySold,
                            accumulator.revenue
                    )
            );
        }

        result.sort(
                Comparator
                        .comparingLong(
                                AnalyticsReportResponse.TopSellingProduct
                                        ::getQuantitySold
                        )
                        .reversed()
                        .thenComparing(
                                AnalyticsReportResponse.TopSellingProduct
                                        ::getRevenue,
                                Comparator.reverseOrder()
                        )
        );

        return result;
    }

    private AnalyticsReportResponse.InventoryMovementSummary
    calculateInventoryMovement(
            LocalDateTime start,
            LocalDateTime endExclusive) {

        List<InventoryTransaction> transactions =
                transactionRepository
                        .findByTransactionDateGreaterThanEqualAndTransactionDateLessThan(
                                start,
                                endExclusive
                        );

        AnalyticsReportResponse.InventoryMovementSummary summary =
                new AnalyticsReportResponse.InventoryMovementSummary();

        long purchased = 0;
        long sold = 0;
        long returned = 0;
        long damaged = 0;
        long expired = 0;
        long adjustedPositive = 0;
        long adjustedNegative = 0;

        for (InventoryTransaction transaction : transactions) {

            int quantity = transaction.getQuantity() == null
                    ? 0
                    : transaction.getQuantity();

            int change = transaction.getQuantityChange() == null
                    ? 0
                    : transaction.getQuantityChange();

            InventoryTransactionType type = transaction.getType();

            if (type == null) {
                continue;
            }

            switch (type) {
                case PURCHASE -> purchased += quantity;
                case SALE -> sold += quantity;
                case RETURN -> returned += quantity;
                case DAMAGE -> damaged += quantity;
                case EXPIRED -> expired += quantity;

                case ADJUSTMENT -> {
                    if (change > 0) {
                        adjustedPositive += change;
                    } else if (change < 0) {
                        adjustedNegative += Math.abs((long) change);
                    }
                }
            }
        }

        summary.setPurchased(purchased);
        summary.setSold(sold);
        summary.setReturned(returned);
        summary.setDamaged(damaged);
        summary.setExpired(expired);
        summary.setAdjusted(adjustedPositive - adjustedNegative);

        return summary;
    }

    private AnalyticsReportResponse.ExpiryRiskSummary
    calculateExpiryRisk() {

        LocalDate today = LocalDate.now();
        LocalDate riskWindowEnd = today.plusDays(EXPIRY_RISK_DAYS);

        List<Batch> batches = batchRepository.findAll();

        long upcomingBatches = 0;
        long upcomingQuantity = 0;

        BigDecimal purchaseValueAtRisk = BigDecimal.ZERO;

        for (Batch batch : batches) {

            if (batch.isArchived()) {
                continue;
            }

            if (batch.getQuantity() == null
                    || batch.getQuantity() <= 0) {
                continue;
            }

            LocalDate expiryDate = batch.getExpiryDate();

            if (expiryDate == null
                    || expiryDate.isBefore(today)
                    || expiryDate.isAfter(riskWindowEnd)) {
                continue;
            }

            upcomingBatches++;
            upcomingQuantity += batch.getQuantity();

            if (batch.getPurchasePrice() != null) {
                purchaseValueAtRisk =
                        purchaseValueAtRisk.add(
                                batch.getPurchasePrice().multiply(
                                        BigDecimal.valueOf(
                                                batch.getQuantity()
                                        )
                                )
                        );
            }
        }

        AnalyticsReportResponse.ExpiryRiskSummary summary =
                new AnalyticsReportResponse.ExpiryRiskSummary();

        summary.setUpcomingExpiryBatches(upcomingBatches);
        summary.setUpcomingExpiryQuantity(upcomingQuantity);
        summary.setPurchaseValueAtRisk(purchaseValueAtRisk);

        return summary;
    }

    private static class ProductSalesAccumulator {

        private final Long productId;
        private final String productName;

        private long quantitySold;
        private BigDecimal revenue = BigDecimal.ZERO;

        private ProductSalesAccumulator(
                Long productId,
                String productName) {

            this.productId = productId;
            this.productName = productName;
        }
    }
}