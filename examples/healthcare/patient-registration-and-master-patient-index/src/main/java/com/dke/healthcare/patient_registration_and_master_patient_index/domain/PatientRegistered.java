package com.dke.healthcare.patient_registration_and_master_patient_index.domain;

public record PatientRegistered(String mrn) implements DomainEvent {
    public static final String TOPIC = "patient.registered";

    public String topic() { return TOPIC; }
    public String key() { return mrn; }
}
