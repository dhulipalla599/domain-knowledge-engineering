package com.dke.data_engineering.data_quality_and_validation.domain;

import java.util.Set;

/** Lifecycle of one validation run over an incoming claims file. */
public enum RunStatus {
    RECEIVED, VALIDATING, LOADED, QUARANTINED, FAILED;

    public boolean canTransitionTo(RunStatus next) {
        return switch (this) {
            case RECEIVED -> next == VALIDATING || next == FAILED;
            case VALIDATING -> Set.of(LOADED, QUARANTINED, FAILED).contains(next);
            case QUARANTINED -> next == LOADED;   // a data steward releases the batch
            case LOADED, FAILED -> false;         // terminal
        };
    }
}
