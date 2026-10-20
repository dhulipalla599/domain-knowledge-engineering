package com.dke.telecom.subscriber_onboarding.api;

import com.dke.telecom.subscriber_onboarding.domain.Plan;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import java.time.LocalDate;

public record OnboardingRequest(
        @NotBlank String fullName,
        @NotNull @Past LocalDate dateOfBirth,
        @NotBlank String idDocumentNumber,
        @NotNull Plan plan,
        @NotBlank String iccid) {
}
