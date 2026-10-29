package com.dke.retail.inventory_management.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import java.time.Instant;
import java.util.UUID;

/** A hold on stock for one order line. (orderRef, sku) is unique, which makes reserve calls idempotent. */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"orderRef", "sku"}))
public class Reservation {

    @Id
    private String id = UUID.randomUUID().toString();
    private String orderRef;
    private String sku;
    private int quantity;
    @Enumerated(EnumType.STRING)
    private ReservationStatus status = ReservationStatus.PENDING;
    private Instant expiresAt;

    protected Reservation() {
    }

    public Reservation(String orderRef, String sku, int quantity, Instant expiresAt) {
        this.orderRef = orderRef;
        this.sku = sku;
        this.quantity = quantity;
        this.expiresAt = expiresAt;
    }

    public void confirm() {
        moveTo(ReservationStatus.CONFIRMED);
    }

    public void release() {
        moveTo(ReservationStatus.RELEASED);
    }

    public void expire() {
        moveTo(ReservationStatus.EXPIRED);
    }

    private void moveTo(ReservationStatus next) {
        if (!status.allowedNext().contains(next)) {
            throw new DomainException.InvalidTransition(status, next);
        }
        status = next;
    }

    public String getId() { return id; }
    public String getOrderRef() { return orderRef; }
    public String getSku() { return sku; }
    public int getQuantity() { return quantity; }
    public ReservationStatus getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
}
