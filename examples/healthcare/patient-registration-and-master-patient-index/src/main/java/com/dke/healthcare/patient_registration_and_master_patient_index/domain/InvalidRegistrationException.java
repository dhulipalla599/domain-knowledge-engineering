package com.dke.healthcare.patient_registration_and_master_patient_index.domain;

public class InvalidRegistrationException extends RuntimeException {
    public InvalidRegistrationException(String message) {
        super(message);
    }
}
