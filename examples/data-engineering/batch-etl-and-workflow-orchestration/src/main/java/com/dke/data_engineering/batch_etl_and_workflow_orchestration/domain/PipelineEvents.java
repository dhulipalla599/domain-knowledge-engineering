package com.dke.data_engineering.batch_etl_and_workflow_orchestration.domain;

import java.time.LocalDate;

/** Domain events. Each record stands in for a Kafka topic named in its comment. */
public final class PipelineEvents {

    private PipelineEvents() { }

    /** Topic: etl.run.started */
    public record RunStarted(Long runId, LocalDate businessDate) { }

    /** Topic: etl.run.completed */
    public record RunCompleted(Long runId, LocalDate businessDate, int extracted, int loaded, int rejected) { }

    /** Topic: etl.run.failed */
    public record RunFailed(Long runId, LocalDate businessDate, String reason) { }
}
