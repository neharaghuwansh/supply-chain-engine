package com.supplychain.engine.controller;

import com.supplychain.engine.dto.BatchRequest;
import com.supplychain.engine.dto.ExpiryRiskResponse;
import com.supplychain.engine.entity.Batch;
import com.supplychain.engine.service.BatchService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/batches")
public class BatchController {

    private final BatchService batchService;

    public BatchController(BatchService batchService) {
        this.batchService = batchService;
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping
    public Batch createBatch(@RequestBody BatchRequest request) {
        return batchService.createBatch(request);
    }

    @GetMapping
    public List<Batch> getAllBatches() {
        return batchService.getAllBatches();
    }

    @GetMapping("/{id}")
    public Batch getBatchById(@PathVariable Long id) {
        return batchService.getBatchById(id);
    }

    @GetMapping("/{id}/expiry-risk")
    public ExpiryRiskResponse getExpiryRisk(@PathVariable Long id) {
        return batchService.getExpiryRisk(id);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping("/{id}")
    public Batch updateBatch(
            @PathVariable Long id,
            @RequestBody BatchRequest request) {
        return batchService.updateBatch(id, request);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PatchMapping("/{id}/archive")
    public Batch archiveBatch(@PathVariable Long id) {
        return batchService.archiveBatch(id);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PatchMapping("/{id}/restore")
    public Batch restoreBatch(@PathVariable Long id) {
        return batchService.restoreBatch(id);
    }
}