package com.dke.banking.payments_processing_ach_wire_rtp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/** An instruction to move money out of one of our accounts over a given rail. */
@Entity
@Table(name = "payment_orders")
public class PaymentOrder {

    @Id
    private UUID id;
    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;
    @Column(name = "from_account", nullable = false)
    private String fromAccount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rail rail;
    @Column(name = "beneficiary_name", nullable = false)
    private String beneficiaryName;
    @Column(name = "beneficiary_routing", nullable = false)
    private String beneficiaryRouting;
    @Column(name = "beneficiary_account", nullable = false)
    private String beneficiaryAccount;
    @Column(nullable = false)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;
    @Column(name = "failure_reason")
    private String failureReason;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PaymentOrder() {
    }

    public PaymentOrder(String idempotencyKey, String fromAccount, Rail rail, String beneficiaryName,
                        String beneficiaryRouting, String beneficiaryAccount, BigDecimal amount) {
        this.id = UUID.randomUUID();
        this.idempotencyKey = idempotencyKey;
        this.fromAccount = fromAccount;
        this.rail = rail;
        this.beneficiaryName = beneficiaryName;
        this.beneficiaryRouting = beneficiaryRouting;
        this.beneficiaryAccount = beneficiaryAccount;
        this.amount = amount;
        this.status = PaymentStatus.RECEIVED;
        this.createdAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    public void markValidated() { moveTo(PaymentStatus.VALIDATED); }
    public void markSubmitted() { moveTo(PaymentStatus.SUBMITTED); }
    public void settle() { moveTo(PaymentStatus.SETTLED); }

    public void reject(String reason) {
        moveTo(PaymentStatus.REJECTED);
        this.failureReason = reason;
    }

    public void markReturned(String reason) {
        moveTo(PaymentStatus.RETURNED);
        this.failureReason = reason;
    }

    private void moveTo(PaymentStatus next) {
        if (!status.allowedNext().contains(next)) {
            throw new IllegalStateException("Illegal transition " + status + " -> " + next);
        }
        this.status = next;
    }

    public boolean sameInstructionAs(PaymentOrder other) {
        return fromAccount.equals(other.fromAccount) && rail == other.rail
                && beneficiaryRouting.equals(other.beneficiaryRouting)
                && beneficiaryAccount.equals(other.beneficiaryAccount)
                && amount.compareTo(other.amount) == 0;
    }

    public UUID getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getFromAccount() { return fromAccount; }
    public Rail getRail() { return rail; }
    public String getBeneficiaryName() { return beneficiaryName; }
    public String getBeneficiaryRouting() { return beneficiaryRouting; }
    public String getBeneficiaryAccount() { return beneficiaryAccount; }
    public BigDecimal getAmount() { return amount; }
    public PaymentStatus getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
}
