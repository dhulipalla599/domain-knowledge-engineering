package com.dke.data_engineering.batch_etl_and_workflow_orchestration.integration;

import com.dke.data_engineering.batch_etl_and_workflow_orchestration.domain.PipelineRun.Trigger;
import com.dke.data_engineering.batch_etl_and_workflow_orchestration.service.PipelineException;
import com.dke.data_engineering.batch_etl_and_workflow_orchestration.service.PipelineService;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Loads yesterday's file. Disabled unless etl.schedule.enabled=true. Production: an Airflow/MWAA DAG or EventBridge. */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "etl.schedule.enabled", havingValue = "true")
public class NightlyScheduler {

    private static final Logger log = LoggerFactory.getLogger(NightlyScheduler.class);

    private final PipelineService pipeline;

    public NightlyScheduler(PipelineService pipeline) {
        this.pipeline = pipeline;
    }

    @Scheduled(cron = "${etl.schedule.cron:0 0 2 * * *}")
    public void loadYesterday() {
        try {
            pipeline.run(LocalDate.now().minusDays(1), Trigger.SCHEDULE);
        } catch (PipelineException e) {
            log.warn("Scheduled run skipped: {}", e.getMessage());
        }
    }
}
