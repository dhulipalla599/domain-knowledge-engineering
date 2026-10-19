package com.dke.telecom.subscriber_onboarding.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
public class Subscription {

    @Id
    private UUID id;
    @Column(nullable = false, unique = true)
    private String idempotencyKey;
    private String fullName;
    private LocalDate dateOfBirth;
    private String idDocumentNumber;
    @Enumerated(EnumType.STRING)
    private Plan plan;
    private String iccid;
    private String msisdn;
    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status;
    private String rejectionReason;
    private Instant createdAt;
    private Instant activatedAt;

    protected Subscription() {
    }

    public static Subscription submit(String idempotencyKey, String fullName, LocalDate dateOfBirth,
                                      String idDocumentNumber, Plan plan, String iccid) {
        Subscription s = new Subscription();
        s.id = UUID.randomUUID();
        s.idempotencyKey = idempotencyKey;
        s.fullName = fullName;
        s.dateOfBirth = dateOfBirth;
        s.idDocumentNumber = idDocumentNumber;
        s.plan = plan;
        s.iccid = iccid;
        s.status = SubscriptionStatus.SUBMITTED;
        s.createdAt = Instant.now();
        return s;
    }

    public void markVerified() {
        moveTo(SubscriptionStatus.VERIFIED);
    }

    public void activate(String msisdn) {
        moveTo(SubscriptionStatus.ACTIVE);
        this.msisdn = msisdn;
        this.activatedAt = Instant.now();
    }

    public void reject(String reason) {
        moveTo(SubscriptionStatus.REJECTED);
        this.rejectionReason = reason;
    }

    public void cancel() {
        moveTo(SubscriptionStatus.CANCELLED);
    }

    private void moveTo(SubscriptionStatus next) {
        if (!status.allowedNext().contains(next)) {
            throw new IllegalTransitionException(status, next);
        }
        this.status = next;
    }

    public UUID getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getFullName() { return fullName; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public String getIdDocumentNumber() { return idDocumentNumber; }
    public Plan getPlan() { return plan; }
    public String getIccid() { return iccid; }
    public String getMsisdn() { return msisdn; }
    public SubscriptionStatus getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getActivatedAt() { return activatedAt; }
}
