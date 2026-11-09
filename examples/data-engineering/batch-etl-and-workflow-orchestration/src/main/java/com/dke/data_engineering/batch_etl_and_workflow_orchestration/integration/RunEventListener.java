package com.dke.data_engineering.batch_etl_and_workflow_orchestration.integration;

import com.dke.data_engineering.batch_etl_and_workflow_orchestration.domain.PipelineEvents.RunCompleted;
import com.dke.data_engineering.batch_etl_and_workflow_orchestration.domain.PipelineEvents.RunFailed;
import com.dke.data_engineering.batch_etl_and_workflow_orchestration.domain.PipelineEvents.RunStarted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Consumes the run events. In production these are Kafka consumers (audit, alerting, downstream triggers). */
@Component
public class RunEventListener {

    private static final Logger log = LoggerFactory.getLogger(RunEventListener.class);

    private final Notifier notifier;

    public RunEventListener(Notifier notifier) {
        this.notifier = notifier;
    }

    @EventListener
    public void onStarted(RunStarted e) {
        log.info("etl.run.started run={} date={}", e.runId(), e.businessDate());
    }

    @EventListener
    public void onCompleted(RunCompleted e) {
        log.info("etl.run.completed run={} extracted={} loaded={} rejected={}",
                e.runId(), e.extracted(), e.loaded(), e.rejected());
    }

    @EventListener
    public void onFailed(RunFailed e) {
        notifier.alert("Run " + e.runId() + " for " + e.businessDate() + " failed: " + e.reason());
    }
}
