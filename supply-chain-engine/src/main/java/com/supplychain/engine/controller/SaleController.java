package com.supplychain.engine.controller;

import com.supplychain.engine.dto.ReturnRequest;
import com.supplychain.engine.dto.SaleRequest;
import com.supplychain.engine.entity.InventoryTransaction;
import com.supplychain.engine.entity.Sale;
import com.supplychain.engine.service.SaleService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/sales")
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'CASHIER')")
    public Sale createSale(@RequestBody SaleRequest request) {
        return saleService.createSale(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'CASHIER')")
    public List<Sale> getAllSales() {
        return saleService.getAllSales();
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('MANAGER', 'CASHIER')")
    public Page<Sale> searchSales(
            @RequestParam(required = false) String search,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (page < 0) {
            throw new IllegalArgumentException("Page cannot be negative");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 100");
        }

        if (fromDate != null && toDate != null
                && fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException(
                    "Start date cannot be after end date");
        }

        LocalDateTime startDate = fromDate == null
                ? null : fromDate.atStartOfDay();

        LocalDateTime endDate = toDate == null
                ? null : toDate.plusDays(1).atStartOfDay();

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "saleDate")
        );

        return saleService.searchSales(
                search,
                startDate,
                endDate,
                pageable
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'CASHIER')")
    public Sale getSale(@PathVariable Long id) {
        return saleService.getSaleById(id);
    }

    @PostMapping("/returns")
    @PreAuthorize("hasAnyRole('MANAGER', 'CASHIER')")
    public InventoryTransaction processReturn(
            @RequestBody ReturnRequest request) {
        return saleService.processReturn(request);
    }

    @GetMapping("/{saleId}/returns")
    @PreAuthorize("hasAnyRole('MANAGER', 'CASHIER')")
    public List<InventoryTransaction> getSaleReturns(
            @PathVariable Long saleId) {
        return saleService.getSaleReturns(saleId);
    }
}