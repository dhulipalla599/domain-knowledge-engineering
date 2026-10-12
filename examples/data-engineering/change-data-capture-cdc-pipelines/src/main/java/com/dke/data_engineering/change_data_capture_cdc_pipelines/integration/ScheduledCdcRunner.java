package com.dke.data_engineering.change_data_capture_cdc_pipelines.integration;

import com.dke.data_engineering.change_data_capture_cdc_pipelines.service.CdcPipelineService;
import com.dke.data_engineering.change_data_capture_cdc_pipelines.service.RunAlreadyInProgressException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** Off by default. Enable with cdc.schedule.enabled=true. Production would run this as a Kubernetes CronJob. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "cdc.schedule.enabled", havingValue = "true")
public class ScheduledCdcRunner {

    private static final Logger log = LoggerFactory.getLogger(ScheduledCdcRunner.class);

    private final CdcPipelineService pipeline;
    private final String source;

    public ScheduledCdcRunner(CdcPipelineService pipeline, @Value("${cdc.schedule.source}") String source) {
        this.pipeline = pipeline;
        this.source = source;
    }

    @Scheduled(cron = "${cdc.schedule.cron}")
    public void tick() {
        try {
            pipeline.run(source, "SCHEDULE");
        } catch (RunAlreadyInProgressException ex) {
            log.info("Skipping scheduled run: {}", ex.getMessage());
        }
    }
}
