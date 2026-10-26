package com.dke.data_engineering.real_time_streaming_etl_with_kafka.integration;

import com.dke.data_engineering.real_time_streaming_etl_with_kafka.domain.DomainEvents.OrdersDlq;
import com.dke.data_engineering.real_time_streaming_etl_with_kafka.domain.DomainEvents.PipelineRunCompleted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Consumer of orders.dlq and pipeline.run-completed: stands in for alerting and metrics. */
@Component
public class DeadLetterLogger {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterLogger.class);

    @EventListener
    public void on(OrdersDlq event) {
        log.warn("run {} dead-lettered '{}': {}", event.runId(), event.rawLine(), event.reason());
    }

    @EventListener
    public void on(PipelineRunCompleted event) {
        log.info("run {} {} loaded={} duplicates={} rejected={}", event.runId(), event.status(),
                event.loaded(), event.duplicates(), event.rejected());
    }
}
