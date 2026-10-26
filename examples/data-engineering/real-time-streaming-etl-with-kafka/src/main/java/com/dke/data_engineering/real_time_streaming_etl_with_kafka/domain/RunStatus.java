package com.dke.data_engineering.real_time_streaming_etl_with_kafka.domain;

import java.util.Set;

/** Lifecycle of one pipeline run. RUNNING is the only non-terminal state. */
public enum RunStatus {
    RUNNING, SUCCEEDED, COMPLETED_WITH_REJECTS, FAILED;

    public Set<RunStatus> allowedNext() {
        return this == RUNNING ? Set.of(SUCCEEDED, COMPLETED_WITH_REJECTS, FAILED) : Set.of();
    }

    public boolean canTransitionTo(RunStatus next) {
        return allowedNext().contains(next);
    }
}
