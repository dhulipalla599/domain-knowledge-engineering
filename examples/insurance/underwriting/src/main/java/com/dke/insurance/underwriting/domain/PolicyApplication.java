package com.dke.insurance.underwriting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.Instant;

/** A home-insurance application moving through underwriting. */
@Entity
public class PolicyApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    @Column(nullable = false)
    private String applicantName;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal sumInsured;

    private int yearBuilt;
    private boolean floodZone;
    private int priorClaims;
    private int riskScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status = ApplicationStatus.SUBMITTED;

    private String decisionReason;
    private String decidedBy;
    private Instant createdAt = Instant.now();

    protected PolicyApplication() {
    }

    public PolicyApplication(String idempotencyKey, String applicantName, BigDecimal sumInsured,
                             int yearBuilt, boolean floodZone, int priorClaims, int riskScore) {
        this.idempotencyKey = idempotencyKey;
        this.applicantName = applicantName;
        this.sumInsured = sumInsured;
        this.yearBuilt = yearBuilt;
        this.floodZone = floodZone;
        this.priorClaims = priorClaims;
        this.riskScore = riskScore;
    }

    /** The only way status changes, so every caller gets the same transition rules. */
    public void moveTo(ApplicationStatus next, String reason, String actor) {
        if (!status.canMoveTo(next)) {
            throw new IllegalTransitionException(status, next);
        }
        this.status = next;
        this.decisionReason = reason;
        this.decidedBy = actor;
    }

    public Long getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getApplicantName() { return applicantName; }
    public BigDecimal getSumInsured() { return sumInsured; }
    public int getYearBuilt() { return yearBuilt; }
    public boolean isFloodZone() { return floodZone; }
    public int getPriorClaims() { return priorClaims; }
    public int getRiskScore() { return riskScore; }
    public ApplicationStatus getStatus() { return status; }
    public String getDecisionReason() { return decisionReason; }
    public String getDecidedBy() { return decidedBy; }
    public Instant getCreatedAt() { return createdAt; }
}
