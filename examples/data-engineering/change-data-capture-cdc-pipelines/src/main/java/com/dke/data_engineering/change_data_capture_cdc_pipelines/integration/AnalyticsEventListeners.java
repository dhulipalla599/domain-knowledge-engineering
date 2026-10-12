package com.dke.data_engineering.change_data_capture_cdc_pipelines.integration;

import com.dke.data_engineering.change_data_capture_cdc_pipelines.domain.DomainEvents.CustomerChangeApplied;
import com.dke.data_engineering.change_data_capture_cdc_pipelines.domain.DomainEvents.PipelineRunCompleted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Stand-ins for Kafka consumers: downstream cache invalidation and the monitoring/alerting service. */
@Component
public class AnalyticsEventListeners {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsEventListeners.class);

    @EventListener
    void onCustomerChangeApplied(CustomerChangeApplied event) {
        log.info("[cdc.customers.applied] key={} op={} lsn={}", event.customerId(), event.op(), event.lsn());
    }

    @EventListener
    void onRunCompleted(PipelineRunCompleted event) {
        log.info("[cdc.pipeline.completed] run={} status={} applied={} skipped={} rejected={}",
                event.runId(), event.status(), event.applied(), event.skippedStale(), event.rejected());
    }
}
