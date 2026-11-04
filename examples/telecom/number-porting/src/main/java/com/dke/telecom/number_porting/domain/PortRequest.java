package com.dke.telecom.number_porting.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "port_request")
public class PortRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 16)
    private String msisdn;

    @Column(nullable = false)
    private String subscriberName;

    @Column(nullable = false)
    private String donorOperator;

    @Column(nullable = false)
    private String recipientOperator;

    @Column(nullable = false)
    private LocalDate requestedPortDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PortStatus status = PortStatus.SUBMITTED;

    private String rejectionReason;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected PortRequest() {
    }

    public PortRequest(String msisdn, String subscriberName, String donorOperator,
                       String recipientOperator, LocalDate requestedPortDate, String idempotencyKey) {
        this.msisdn = msisdn;
        this.subscriberName = subscriberName;
        this.donorOperator = donorOperator;
        this.recipientOperator = recipientOperator;
        this.requestedPortDate = requestedPortDate;
        this.idempotencyKey = idempotencyKey;
    }

    /** The only way status changes: illegal moves are refused here, not in callers. */
    public void transitionTo(PortStatus target) {
        if (!status.canMoveTo(target)) {
            throw new IllegalPortStateException(id, status, target);
        }
        this.status = target;
    }

    public void reject(String reason) {
        transitionTo(PortStatus.REJECTED);
        this.rejectionReason = reason;
    }

    public Long getId() { return id; }
    public String getMsisdn() { return msisdn; }
    public String getSubscriberName() { return subscriberName; }
    public String getDonorOperator() { return donorOperator; }
    public String getRecipientOperator() { return recipientOperator; }
    public LocalDate getRequestedPortDate() { return requestedPortDate; }
    public PortStatus getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getCreatedAt() { return createdAt; }
}
