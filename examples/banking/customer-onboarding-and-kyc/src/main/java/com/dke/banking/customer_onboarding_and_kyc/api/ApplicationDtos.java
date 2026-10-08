package com.dke.banking.customer_onboarding_and_kyc.api;

import com.dke.banking.customer_onboarding_and_kyc.domain.OnboardingApplication;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Request and response bodies for the onboarding API. */
public final class ApplicationDtos {

    private ApplicationDtos() {
    }

    public record SubmitApplicationRequest(
            @NotBlank String legalName,
            @NotNull @Past LocalDate dateOfBirth,
            @NotBlank @Pattern(regexp = "[A-Z]{2}", message = "must be an ISO 3166 alpha-2 code") String nationality,
            @NotBlank String occupation) {
    }

    /** Customer-facing view: generic status only, never screening details. */
    public record ApplicationResponse(UUID applicationId, String status, String nextStep, String accountNumber) {
    }

    public record ReviewDecisionRequest(@NotNull Boolean cleared, @NotBlank String reason) {
    }

    /** Analyst view used by the review queue. */
    public record ReviewCaseResponse(UUID applicationId, String legalName, String status,
                                     String screeningOutcome, String riskRating, Instant submittedAt) {

        static ReviewCaseResponse from(OnboardingApplication a) {
            return new ReviewCaseResponse(a.getId(), a.getLegalName(), a.getStatus().name(),
                    a.getScreeningOutcome() == null ? null : a.getScreeningOutcome().name(),
                    a.getRiskRating() == null ? null : a.getRiskRating().name(), a.getCreatedAt());
        }
    }
}
