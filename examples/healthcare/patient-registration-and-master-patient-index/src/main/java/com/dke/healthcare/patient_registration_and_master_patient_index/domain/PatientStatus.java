package com.dke.healthcare.patient_registration_and_master_patient_index.domain;

import java.util.EnumSet;
import java.util.Set;

/** Lifecycle of a master-index record. MERGED is terminal: the record only points at its survivor. */
public enum PatientStatus {
    ACTIVE, PENDING_REVIEW, MERGED;

    public Set<PatientStatus> allowedNext() {
        return switch (this) {
            case PENDING_REVIEW -> EnumSet.of(ACTIVE, MERGED);
            case ACTIVE -> EnumSet.of(MERGED);
            case MERGED -> EnumSet.noneOf(PatientStatus.class);
        };
    }
}
