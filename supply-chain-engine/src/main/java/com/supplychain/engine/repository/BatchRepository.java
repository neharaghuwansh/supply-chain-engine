 package com.supplychain.engine.repository;

import com.supplychain.engine.entity.Batch;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BatchRepository extends JpaRepository<Batch, Long> {

    List<Batch> findByProductIdAndQuantityGreaterThanAndArchivedFalseOrderByExpiryDateAsc(
            Long productId,
            Integer quantity
    );

    /*
     * Retrieves non-archived batches with positive stock in FEFO order,
     * while acquiring database write locks on the selected rows.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT b
            FROM Batch b
            WHERE b.product.id = :productId
              AND b.quantity > 0
              AND b.archived = false
            ORDER BY b.expiryDate ASC, b.id ASC
            """)
    List<Batch> findAvailableBatchesForUpdate(
            @Param("productId") Long productId
    );

    /*
     * Locks an individual batch before stock modification.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Batch b WHERE b.id = :id")
    Optional<Batch> findByIdForUpdate(@Param("id") Long id);
}

