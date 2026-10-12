package com.dke.data_engineering.change_data_capture_cdc_pipelines.domain;

/** Domain events. Each stands in for a message on an Apache Kafka topic (named in the comment). */
public final class DomainEvents {

    private DomainEvents() {
    }

    /** Topic: cdc.customers.applied, keyed by customerId so changes to one row stay ordered. */
    public record CustomerChangeApplied(Long customerId, String op, long lsn) {
    }

    /** Topic: cdc.pipeline.completed. */
    public record PipelineRunCompleted(Long runId, RunStatus status, int applied, int skippedStale, int rejected) {
    }
}
