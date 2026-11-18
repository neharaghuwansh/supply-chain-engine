package com.supplychain.engine.scheduler;

import com.supplychain.engine.service.ExpiryProcessingService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExpiryScheduler {

    private final ExpiryProcessingService expiryProcessingService;

    public ExpiryScheduler(
            ExpiryProcessingService expiryProcessingService) {
        this.expiryProcessingService = expiryProcessingService;
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Kolkata")
    public void processExpiredBatches() {

        expiryProcessingService.processExpiredBatches();
    }
}