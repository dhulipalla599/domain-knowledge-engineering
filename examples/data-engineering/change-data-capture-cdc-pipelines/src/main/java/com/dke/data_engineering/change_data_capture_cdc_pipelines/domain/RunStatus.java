package com.dke.data_engineering.change_data_capture_cdc_pipelines.domain;

/** Lifecycle of one pipeline run. Only the transitions in {@link #canMoveTo} are legal. */
public enum RunStatus {
    PENDING, RUNNING, SUCCEEDED, FAILED;

    public boolean canMoveTo(RunStatus next) {
        return switch (this) {
            case PENDING -> next == RUNNING;
            case RUNNING -> next == SUCCEEDED || next == FAILED;
            case SUCCEEDED, FAILED -> false;
        };
    }
}
