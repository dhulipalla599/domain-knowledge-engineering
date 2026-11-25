package com.dke.data_engineering.data_quality_and_validation.integration;

import com.dke.data_engineering.data_quality_and_validation.service.QualityException;
import com.dke.data_engineering.data_quality_and_validation.service.ValidationService;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Validates yesterday's file every morning. Off by default: set dq.schedule.enabled=true. */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "dq.schedule.enabled", havingValue = "true")
public class DailyFeedScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailyFeedScheduler.class);
    private final ValidationService service;

    public DailyFeedScheduler(ValidationService service) {
        this.service = service;
    }

    @Scheduled(cron = "${dq.schedule.cron}")
    public void validateYesterday() {
        LocalDate date = LocalDate.now().minusDays(1);
        try {
            log.info("Scheduled validation for {} ended {}", date, service.validate(date).getStatus());
        } catch (QualityException e) {
            log.warn("Scheduled validation for {} skipped: {}", date, e.getMessage());
        }
    }
}
