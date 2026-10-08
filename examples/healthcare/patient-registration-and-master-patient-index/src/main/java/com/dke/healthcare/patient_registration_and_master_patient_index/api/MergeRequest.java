package com.dke.healthcare.patient_registration_and_master_patient_index.api;

import jakarta.validation.constraints.NotBlank;

public record MergeRequest(@NotBlank String survivorMrn) {
}
