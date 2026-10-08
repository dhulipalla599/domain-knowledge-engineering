package com.dke.healthcare.patient_registration_and_master_patient_index.domain;

public record MatchReviewRequested(String mrn, String candidateMrn, int score) implements DomainEvent {
    public static final String TOPIC = "patient.match-review-requested";

    public String topic() { return TOPIC; }
    public String key() { return mrn; }
}
