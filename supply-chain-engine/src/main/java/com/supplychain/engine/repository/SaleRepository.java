package com.supplychain.engine.repository;

import com.supplychain.engine.entity.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    @Query(
            value = "SELECT pg_advisory_xact_lock(:lockId) IS NULL",
            nativeQuery = true
    )
    boolean acquireIdempotencyLock(@Param("lockId") long lockId);

    @EntityGraph(attributePaths = {"items", "items.product", "items.batch"})
    Optional<Sale> findByIdempotencyKey(String idempotencyKey);

    List<Sale> findBySaleDateBetween(
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    List<Sale> findBySaleDateGreaterThanEqualAndSaleDateLessThan(
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    Page<Sale> findAllByOrderBySaleDateDesc(Pageable pageable);

    Page<Sale> findBySaleDateGreaterThanEqualAndSaleDateLessThanOrderBySaleDateDesc(
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"items", "items.product", "items.batch"})
    @Query(
            value = """
            SELECT DISTINCT s
            FROM Sale s
            LEFT JOIN s.items i
            LEFT JOIN i.product p
            WHERE (CAST(:startDate AS timestamp) IS NULL
                   OR s.saleDate >= :startDate)
              AND (CAST(:endDate AS timestamp) IS NULL
                   OR s.saleDate < :endDate)
              AND (
                    :search IS NULL
                    OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR CAST(s.id AS string) = :search
                  )
            """,
            countQuery = """
            SELECT COUNT(DISTINCT s.id)
            FROM Sale s
            LEFT JOIN s.items i
            LEFT JOIN i.product p
            WHERE (CAST(:startDate AS timestamp) IS NULL
                   OR s.saleDate >= :startDate)
              AND (CAST(:endDate AS timestamp) IS NULL
                   OR s.saleDate < :endDate)
              AND (
                    :search IS NULL
                    OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR CAST(s.id AS string) = :search
                  )
            """
    )
    Page<Sale> searchSales(
            @Param("search") String search,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}