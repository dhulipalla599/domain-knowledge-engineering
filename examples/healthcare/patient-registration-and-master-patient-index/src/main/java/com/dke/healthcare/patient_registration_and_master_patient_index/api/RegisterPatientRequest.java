package com.dke.healthcare.patient_registration_and_master_patient_index.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record RegisterPatientRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotNull LocalDate dateOfBirth,
        String phone,
        String postcode) {
}
