package com.dke.healthcare.patient_registration_and_master_patient_index.domain;

public record PatientsMerged(String mrn, String survivorMrn) implements DomainEvent {
    public static final String TOPIC = "patient.merged";

    public String topic() { return TOPIC; }
    public String key() { return survivorMrn; }
}
