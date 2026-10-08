package com.dke.banking.customer_onboarding_and_kyc.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** One person's request to become a customer. The aggregate root of onboarding. */
@Entity
@Table(name = "onboarding_application")
public class OnboardingApplication {

    @Id
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "legal_name", nullable = false)
    private String legalName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "nationality", nullable = false, length = 2)
    private String nationality;

    @Column(name = "occupation", nullable = false)
    private String occupation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApplicationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "screening_outcome", length = 20)
    private ScreeningOutcome screeningOutcome;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_rating", length = 10)
    private RiskRating riskRating;

    @Column(name = "decision_reason")
    private String decisionReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    @Column(name = "version")
    private long version;

    protected OnboardingApplication() {
        // for JPA
    }

    public OnboardingApplication(String idempotencyKey, String legalName, LocalDate dateOfBirth,
                                 String nationality, String occupation) {
        this.id = UUID.randomUUID();
        this.idempotencyKey = idempotencyKey;
        this.legalName = legalName;
        this.dateOfBirth = dateOfBirth;
        this.nationality = nationality;
        this.occupation = occupation;
        this.status = ApplicationStatus.SUBMITTED;
        this.createdAt = Instant.now();
    }

    /** Screening has started. */
    public void startScreening() {
        moveTo(ApplicationStatus.SCREENING);
    }

    /** Records the screening result and risk rating, then applies the policy's decision. */
    public Decision completeScreening(ScreeningOutcome outcome, RiskRating rating, Decision decision) {
        this.screeningOutcome = outcome;
        this.riskRating = rating;
        switch (decision) {
            case APPROVE -> moveTo(ApplicationStatus.APPROVED);
            case REVIEW -> moveTo(ApplicationStatus.IN_REVIEW);
            case REJECT -> reject("Screening: " + outcome);
        }
        return decision;
    }

    /** An analyst clears or confirms the review. A reason is always required for the audit trail. */
    public void resolveReview(boolean cleared, String reason) {
        if (status != ApplicationStatus.IN_REVIEW) {
            throw new BusinessRuleViolation("NOT_IN_REVIEW", "Application " + id + " is not waiting for review");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleViolation("REASON_REQUIRED", "A review decision needs a reason");
        }
        this.decisionReason = reason;
        moveTo(cleared ? ApplicationStatus.APPROVED : ApplicationStatus.REJECTED);
    }

    private void reject(String reason) {
        this.decisionReason = reason;
        moveTo(ApplicationStatus.REJECTED);
    }

    private void moveTo(ApplicationStatus target) {
        if (!status.canMoveTo(target)) {
            throw new IllegalStateException("Cannot move from " + status + " to " + target);
        }
        this.status = target;
    }

    public UUID getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getLegalName() {
        return legalName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getNationality() {
        return nationality;
    }

    public String getOccupation() {
        return occupation;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public ScreeningOutcome getScreeningOutcome() {
        return screeningOutcome;
    }

    public RiskRating getRiskRating() {
        return riskRating;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
