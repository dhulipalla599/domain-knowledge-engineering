package com.dke.healthcare.patient_registration_and_master_patient_index.api;

import com.dke.healthcare.patient_registration_and_master_patient_index.domain.Patient;
import com.dke.healthcare.patient_registration_and_master_patient_index.domain.PatientStatus;
import java.time.LocalDate;

public record PatientResponse(String mrn, PatientStatus status, String firstName, String lastName,
                              LocalDate dateOfBirth, String survivorMrn) {
    static PatientResponse from(Patient p) {
        return new PatientResponse(p.getMrn(), p.getStatus(), p.getFirstName(), p.getLastName(),
                p.getDateOfBirth(), p.getSurvivorMrn());
    }
}
