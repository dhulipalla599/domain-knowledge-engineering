package com.dke.healthcare.patient_registration_and_master_patient_index.domain;

/** Stand-in for a Kafka message: topic name plus the partition key that preserves per-patient ordering. */
public sealed interface DomainEvent permits PatientRegistered, MatchReviewRequested, PatientsMerged {
    String topic();

    String key();
}
