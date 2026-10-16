package com.dke.logistics.shipment_booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** A customer's request to move goods on a specific rate quote. */
@Entity
@Table(name = "booking", uniqueConstraints = @UniqueConstraint(columnNames = "idempotencyKey"))
public class Booking {

    @Id
    @GeneratedValue
    private UUID id;
    private String reference;
    private String customerId;
    private String quoteId;
    @Column(nullable = false)
    private String idempotencyKey;
    private double weightKg;
    private String unNumber;
    private BigDecimal price;
    private String currency;
    private String carrierConfirmation;
    private Instant createdAt;
    @Enumerated(EnumType.STRING)
    private BookingStatus status = BookingStatus.PENDING;

    protected Booking() {
    }

    public Booking(String reference, String customerId, RateQuote quote, String idempotencyKey,
                   double weightKg, String unNumber, Instant createdAt) {
        this.reference = reference;
        this.customerId = customerId;
        this.quoteId = quote.getId();
        this.idempotencyKey = idempotencyKey;
        this.weightKg = weightKg;
        this.unNumber = unNumber;
        this.price = quote.getPrice();
        this.currency = quote.getCurrency();
        this.createdAt = createdAt;
    }

    public void confirm(String carrierConfirmation) {
        moveTo(BookingStatus.CONFIRMED);
        this.carrierConfirmation = carrierConfirmation;
    }

    public void cancel() {
        moveTo(BookingStatus.CANCELLED);
    }

    public void dispatch() {
        moveTo(BookingStatus.DISPATCHED);
    }

    private void moveTo(BookingStatus next) {
        if (!status.allowedNext().contains(next)) {
            throw new IllegalStateException("Illegal transition " + status + " -> " + next);
        }
        this.status = next;
    }

    public UUID getId() { return id; }
    public String getReference() { return reference; }
    public String getCustomerId() { return customerId; }
    public String getQuoteId() { return quoteId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public double getWeightKg() { return weightKg; }
    public String getUnNumber() { return unNumber; }
    public BigDecimal getPrice() { return price; }
    public String getCurrency() { return currency; }
    public String getCarrierConfirmation() { return carrierConfirmation; }
    public Instant getCreatedAt() { return createdAt; }
    public BookingStatus getStatus() { return status; }
}
