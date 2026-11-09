package com.dke.data_engineering.batch_etl_and_workflow_orchestration.domain;

import java.util.EnumSet;
import java.util.Set;

/** Lifecycle of one pipeline run. SUCCEEDED and FAILED are terminal. */
public enum RunStatus {
    PENDING, EXTRACTING, TRANSFORMING, LOADING, SUCCEEDED, FAILED;

    public static final Set<RunStatus> ACTIVE = EnumSet.of(PENDING, EXTRACTING, TRANSFORMING, LOADING);

    public boolean canTransitionTo(RunStatus next) {
        return switch (this) {
            case PENDING -> next == EXTRACTING || next == FAILED;
            case EXTRACTING -> next == TRANSFORMING || next == FAILED;
            case TRANSFORMING -> next == LOADING || next == FAILED;
            case LOADING -> next == SUCCEEDED || next == FAILED;
            case SUCCEEDED, FAILED -> false;
        };
    }
}
