package com.dke.healthcare.patient_registration_and_master_patient_index.domain;

public class PatientNotFoundException extends RuntimeException {
    public PatientNotFoundException(String mrn) {
        super("No patient with MRN " + mrn);
    }
}
