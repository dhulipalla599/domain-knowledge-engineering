package com.dke.banking.customer_onboarding_and_kyc.service;

import com.dke.banking.customer_onboarding_and_kyc.domain.BusinessRuleViolation;
import com.dke.banking.customer_onboarding_and_kyc.domain.Decision;
import com.dke.banking.customer_onboarding_and_kyc.domain.OnboardingApplication;
import com.dke.banking.customer_onboarding_and_kyc.domain.OnboardingEvents.ApplicationApproved;
import com.dke.banking.customer_onboarding_and_kyc.domain.OnboardingEvents.ApplicationRejected;
import com.dke.banking.customer_onboarding_and_kyc.domain.OnboardingEvents.ApplicationSubmitted;
import com.dke.banking.customer_onboarding_and_kyc.domain.OnboardingEvents.ScreeningCompleted;
import com.dke.banking.customer_onboarding_and_kyc.domain.RiskRating;
import com.dke.banking.customer_onboarding_and_kyc.domain.ScreeningOutcome;
import com.dke.banking.customer_onboarding_and_kyc.domain.ApplicationStatus;
import com.dke.banking.customer_onboarding_and_kyc.integration.SanctionsScreeningClient;
import com.dke.banking.customer_onboarding_and_kyc.repository.OnboardingApplicationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Orchestrates onboarding: submit, screen, rate, decide, and analyst review. */
@Service
public class OnboardingService {

    static final int MINIMUM_AGE = 18;

    private final OnboardingApplicationRepository applications;
    private final SanctionsScreeningClient screening;
    private final RiskRatingPolicy riskPolicy;
    private final OnboardingDecisionPolicy decisionPolicy;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public OnboardingService(OnboardingApplicationRepository applications, SanctionsScreeningClient screening,
                             RiskRatingPolicy riskPolicy, OnboardingDecisionPolicy decisionPolicy,
                             ApplicationEventPublisher events, Clock clock) {
        this.applications = applications;
        this.screening = screening;
        this.riskPolicy = riskPolicy;
        this.decisionPolicy = decisionPolicy;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Submits an application. Replaying the same idempotency key returns the original
     * application instead of creating a second customer (mobile clients retry).
     */
    @Transactional
    public OnboardingApplication submit(String idempotencyKey, String legalName, LocalDate dateOfBirth,
                                        String nationality, String occupation) {
        var existing = applications.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }
        int age = Period.between(dateOfBirth, LocalDate.now(clock)).getYears();
        if (age < MINIMUM_AGE) {
            throw new BusinessRuleViolation("UNDER_MINIMUM_AGE",
                    "Applicants must be at least " + MINIMUM_AGE + "; minors need a guardian-led application");
        }

        var application = applications.save(
                new OnboardingApplication(idempotencyKey, legalName, dateOfBirth, nationality, occupation));
        events.publishEvent(new ApplicationSubmitted(application.getId(), now()));

        application.startScreening();
        ScreeningOutcome outcome = screening.screen(legalName, dateOfBirth);
        RiskRating rating = riskPolicy.rate(nationality, occupation);
        Decision decision = application.completeScreening(outcome, rating, decisionPolicy.decide(outcome, rating));
        events.publishEvent(new ScreeningCompleted(application.getId(), outcome, rating, now()));
        publishOutcome(application, decision);
        return application;
    }

    /** An analyst clears or confirms a screening hit / EDD case. */
    @Transactional
    public OnboardingApplication resolveReview(UUID id, boolean cleared, String reason) {
        var application = get(id);
        application.resolveReview(cleared, reason);
        publishOutcome(application, cleared ? Decision.APPROVE : Decision.REJECT);
        return application;
    }

    @Transactional(readOnly = true)
    public OnboardingApplication get(UUID id) {
        return applications.findById(id).orElseThrow(() -> new ApplicationNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<OnboardingApplication> reviewQueue() {
        return applications.findByStatusOrderByCreatedAtAsc(ApplicationStatus.IN_REVIEW);
    }

    private void publishOutcome(OnboardingApplication application, Decision decision) {
        switch (decision) {
            case APPROVE -> events.publishEvent(
                    new ApplicationApproved(application.getId(), application.getRiskRating(), now()));
            case REJECT -> events.publishEvent(
                    new ApplicationRejected(application.getId(), application.getDecisionReason(), now()));
            case REVIEW -> {
                // nothing to publish: the case now sits in the analyst queue
            }
        }
    }

    private Instant now() {
        return clock.instant();
    }
}
