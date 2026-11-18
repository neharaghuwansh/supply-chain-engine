package com.supplychain.engine.repository;

import com.supplychain.engine.entity.InventoryTransaction;
import com.supplychain.engine.entity.InventoryTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface InventoryTransactionRepository
        extends JpaRepository<InventoryTransaction, Long> {

    @Query("""
            SELECT COALESCE(SUM(t.quantity), 0)
            FROM InventoryTransaction t
            WHERE t.saleItem.id = :saleItemId
              AND t.type = :type
            """)
    Integer sumQuantityBySaleItemIdAndType(
            @Param("saleItemId") Long saleItemId,
            @Param("type") InventoryTransactionType type
    );

    @Query("""
            SELECT t
            FROM InventoryTransaction t
            JOIN t.saleItem si
            JOIN si.sale s
            WHERE s.id = :saleId
              AND t.type = :type
            ORDER BY t.transactionDate DESC
            """)
    List<InventoryTransaction> findReturnsBySaleIdAndType(
            @Param("saleId") Long saleId,
            @Param("type") InventoryTransactionType type
    );

    List<InventoryTransaction> findByTransactionDateBetween(
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    List<InventoryTransaction>
    findByTransactionDateGreaterThanEqualAndTransactionDateLessThan(
            LocalDateTime startDate,
            LocalDateTime endDate
    );
}

