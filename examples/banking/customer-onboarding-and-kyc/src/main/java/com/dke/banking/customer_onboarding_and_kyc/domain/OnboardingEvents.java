package com.dke.banking.customer_onboarding_and_kyc.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain events. In production each one is a Kafka topic keyed by applicationId
 * (application.submitted, screening.completed, application.approved, application.rejected).
 */
public final class OnboardingEvents {

    private OnboardingEvents() {
    }

    public record ApplicationSubmitted(UUID applicationId, Instant at) {
    }

    public record ScreeningCompleted(UUID applicationId, ScreeningOutcome outcome, RiskRating rating, Instant at) {
    }

    public record ApplicationApproved(UUID applicationId, RiskRating rating, Instant at) {
    }

    public record ApplicationRejected(UUID applicationId, String internalReason, Instant at) {
    }
}
