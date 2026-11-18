package com.supplychain.engine.controller;

import com.supplychain.engine.dto.AnalyticsReportResponse;
import com.supplychain.engine.dto.AnalyticsResponse;
import com.supplychain.engine.service.AnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    // Existing Dashboard endpoint — preserved
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping
    public AnalyticsResponse getAnalytics() {
        return analyticsService.getDashboardAnalytics();
    }

    // New detailed reports endpoint
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/reports")
    public AnalyticsReportResponse getReports(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate) {

        return analyticsService.getReports(startDate, endDate);
    }
}