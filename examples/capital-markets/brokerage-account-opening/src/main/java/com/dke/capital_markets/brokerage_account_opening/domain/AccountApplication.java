package com.dke.capital_markets.brokerage_account_opening.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "account_application")
public class AccountApplication {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "applicant_name", nullable = false)
    private String applicantName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    /** Only the last four digits of the tax id are kept; the full value never reaches this service's database. */
    @Column(name = "tax_id_last4", nullable = false, length = 4)
    private String taxIdLast4;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;

    @Column(name = "liquid_net_worth", nullable = false)
    private BigDecimal liquidNetWorth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    @Column(name = "decision_reason")
    private String decisionReason;

    @Column(name = "account_number", unique = true)
    private String accountNumber;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AccountApplication() {
    }

    public AccountApplication(String idempotencyKey, String applicantName, LocalDate dateOfBirth,
                              String taxIdLast4, AccountType accountType, BigDecimal liquidNetWorth) {
        this.idempotencyKey = idempotencyKey;
        this.applicantName = applicantName;
        this.dateOfBirth = dateOfBirth;
        this.taxIdLast4 = taxIdLast4;
        this.accountType = accountType;
        this.liquidNetWorth = liquidNetWorth;
        this.status = ApplicationStatus.SUBMITTED;
        this.createdAt = Instant.now();
    }

    public void approve(String reason) {
        moveTo(ApplicationStatus.APPROVED, reason);
    }

    public void sendToReview(String reason) {
        moveTo(ApplicationStatus.PENDING_REVIEW, reason);
    }

    public void reject(String reason) {
        moveTo(ApplicationStatus.REJECTED, reason);
    }

    public void activate(String accountNumber) {
        moveTo(ApplicationStatus.ACTIVE, decisionReason);
        this.accountNumber = accountNumber;
    }

    /** The only place the status changes, so an illegal jump can never be persisted. */
    private void moveTo(ApplicationStatus next, String reason) {
        if (!status.canMoveTo(next)) {
            throw new IllegalStateException("Cannot move application from " + status + " to " + next);
        }
        this.status = next;
        this.decisionReason = reason;
    }

    public UUID getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getApplicantName() { return applicantName; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public String getTaxIdLast4() { return taxIdLast4; }
    public AccountType getAccountType() { return accountType; }
    public BigDecimal getLiquidNetWorth() { return liquidNetWorth; }
    public ApplicationStatus getStatus() { return status; }
    public String getDecisionReason() { return decisionReason; }
    public String getAccountNumber() { return accountNumber; }
    public Instant getCreatedAt() { return createdAt; }
}
