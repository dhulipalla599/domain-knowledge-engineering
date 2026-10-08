package com.dke.healthcare.patient_registration_and_master_patient_index.domain;

public class IllegalStateTransitionException extends RuntimeException {
    public IllegalStateTransitionException(String mrn, PatientStatus from, PatientStatus to) {
        super("Record %s cannot move from %s to %s".formatted(mrn, from, to));
    }
}
