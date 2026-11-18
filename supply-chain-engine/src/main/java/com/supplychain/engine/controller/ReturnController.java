package com.supplychain.engine.controller;

import com.supplychain.engine.dto.ReturnRequest;
import com.supplychain.engine.entity.InventoryTransaction;
import com.supplychain.engine.service.SaleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/returns")
public class ReturnController {

    private final SaleService saleService;

    public ReturnController(SaleService saleService) {
        this.saleService = saleService;
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'CASHIER')")
    @PostMapping
    public InventoryTransaction processReturn(
            @RequestBody ReturnRequest request) {
        return saleService.processReturn(request);
    }
}