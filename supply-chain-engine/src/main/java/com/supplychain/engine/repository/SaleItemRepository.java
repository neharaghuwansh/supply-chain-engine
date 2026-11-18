package com.supplychain.engine.repository;

import com.supplychain.engine.entity.SaleItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT si FROM SaleItem si WHERE si.id = :id")
    Optional<SaleItem> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT si
            FROM SaleItem si
            JOIN FETCH si.sale s
            JOIN FETCH si.product p
            WHERE s.saleDate >= :startDate
              AND s.saleDate < :endDate
            """)
    List<SaleItem> findSaleItemsForReport(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}