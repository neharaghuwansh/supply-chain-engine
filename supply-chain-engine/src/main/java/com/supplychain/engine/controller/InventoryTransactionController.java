package com.supplychain.engine.controller;

import com.supplychain.engine.dto.StockAdjustmentRequest;
import com.supplychain.engine.entity.InventoryTransaction;
import com.supplychain.engine.repository.InventoryTransactionRepository;
import com.supplychain.engine.service.InventoryTransactionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory-transactions")
public class InventoryTransactionController {

    private final InventoryTransactionRepository transactionRepository;
    private final InventoryTransactionService inventoryTransactionService;

    public InventoryTransactionController(
            InventoryTransactionRepository transactionRepository,
            InventoryTransactionService inventoryTransactionService) {

        this.transactionRepository = transactionRepository;
        this.inventoryTransactionService = inventoryTransactionService;
    }

    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping
    public List<InventoryTransaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping("/adjust")
    public InventoryTransaction adjustStock(
            @RequestBody StockAdjustmentRequest request) {

        if (request.getBatchId() == null) {
            throw new IllegalArgumentException(
                    "Batch ID is required"
            );
        }

        if (request.getQuantity() == null) {
            throw new IllegalArgumentException(
                    "Adjustment quantity is required"
            );
        }

        return inventoryTransactionService.adjustStock(
                request.getBatchId(),
                request.getQuantity(),
                "MANAGER-STOCK-ADJUSTMENT"
        );
    }
}