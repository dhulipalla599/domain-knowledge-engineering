package com.dke.data_engineering.data_quality_and_validation.integration;

import com.dke.data_engineering.data_quality_and_validation.domain.QualityEvents.BatchFailed;
import com.dke.data_engineering.data_quality_and_validation.domain.QualityEvents.BatchLoaded;
import com.dke.data_engineering.data_quality_and_validation.domain.QualityEvents.BatchQuarantined;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Stand-in consumers for the Kafka topics dq.batch.loaded, dq.batch.quarantined and dq.batch.failed.
 * In production: downstream jobs start on "loaded"; the data steward on-call is paged on "quarantined"/"failed".
 */
@Component
public class QualityEventListener {

    private static final Logger log = LoggerFactory.getLogger(QualityEventListener.class);

    @EventListener
    public void onLoaded(BatchLoaded e) {
        log.info("[dq.batch.loaded] run={} file={} loaded={} rejected={} released={} -> reserving job may start",
                e.runId(), e.fileName(), e.rowsLoaded(), e.rowsRejected(), e.released());
    }

    @EventListener
    public void onQuarantined(BatchQuarantined e) {
        log.warn("[dq.batch.quarantined] run={} file={} errorRatio={} -> alert data steward: {}",
                e.runId(), e.fileName(), e.errorRatio(), e.reason());
    }

    @EventListener
    public void onFailed(BatchFailed e) {
        log.error("[dq.batch.failed] run={} file={} -> ask the sender for a corrected file: {}",
                e.runId(), e.fileName(), e.reason());
    }
}
