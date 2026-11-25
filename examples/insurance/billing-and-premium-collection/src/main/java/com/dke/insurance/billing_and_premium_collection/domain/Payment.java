package com.dke.insurance.billing_and_premium_collection.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/** A captured premium payment. The unique idempotency key stops a retried request charging twice. */
@Entity
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String idempotencyKey;
    private Long installmentId;
    @Column(precision = 12, scale = 2)
    private BigDecimal amount;
    private String gatewayReference;
    private Instant receivedAt;

    protected Payment() {
    }

    public Payment(String idempotencyKey, Long installmentId, BigDecimal amount,
                   String gatewayReference, Instant receivedAt) {
        this.idempotencyKey = idempotencyKey;
        this.installmentId = installmentId;
        this.amount = amount;
        this.gatewayReference = gatewayReference;
        this.receivedAt = receivedAt;
    }

    /** True when a retry carries the same key but a different request body. */
    public boolean conflictsWith(Long otherInstallmentId, BigDecimal otherAmount) {
        return !installmentId.equals(otherInstallmentId) || amount.compareTo(otherAmount) != 0;
    }

    public Long getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Long getInstallmentId() { return installmentId; }
    public BigDecimal getAmount() { return amount; }
    public String getGatewayReference() { return gatewayReference; }
    public Instant getReceivedAt() { return receivedAt; }
}
