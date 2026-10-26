package com.dke.data_engineering.real_time_streaming_etl_with_kafka.integration;

import com.dke.data_engineering.real_time_streaming_etl_with_kafka.service.StreamingEtlService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Disabled by default. Enable with etl.schedule.enabled=true. */
@Component
@ConditionalOnProperty(name = "etl.schedule.enabled", havingValue = "true")
public class ScheduledEtlRunner {

    private final StreamingEtlService etl;
    private final String source;

    public ScheduledEtlRunner(StreamingEtlService etl, @Value("${etl.schedule.source:orders-batch-1}") String source) {
        this.etl = etl;
        this.source = source;
    }

    @Scheduled(fixedDelayString = "${etl.schedule.delay-ms:60000}")
    public void tick() {
        etl.run(source);
    }
}
