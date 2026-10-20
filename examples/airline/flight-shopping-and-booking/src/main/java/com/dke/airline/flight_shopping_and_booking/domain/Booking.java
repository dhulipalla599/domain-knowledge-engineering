package com.dke.airline.flight_shopping_and_booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "booking")
public class Booking {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "offer_id", nullable = false)
    private UUID offerId;

    @Column(name = "passenger_name", nullable = false)
    private String passengerName;

    @Column(name = "passenger_email", nullable = false)
    private String passengerEmail;

    /** Price agreed at hold time. Later fare changes never touch an existing booking. */
    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(name = "hold_expires_at", nullable = false)
    private Instant holdExpiresAt;

    @Column(length = 6)
    private String pnr;

    @Column(name = "ticket_number")
    private String ticketNumber;

    @Column(name = "status_reason")
    private String statusReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Booking() {
    }

    public Booking(String idempotencyKey, UUID offerId, String passengerName, String passengerEmail,
                   BigDecimal totalAmount, String currency, Instant now, Instant holdExpiresAt) {
        this.idempotencyKey = idempotencyKey;
        this.offerId = offerId;
        this.passengerName = passengerName;
        this.passengerEmail = passengerEmail;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.status = BookingStatus.HELD;
        this.createdAt = now;
        this.holdExpiresAt = holdExpiresAt;
    }

    public boolean isHoldExpired(Instant now) {
        return status == BookingStatus.HELD && !now.isBefore(holdExpiresAt);
    }

    public void confirmPayment() {
        moveTo(BookingStatus.CONFIRMED, "Payment authorised");
    }

    public void issueTicket(String pnr, String ticketNumber) {
        moveTo(BookingStatus.TICKETED, "Ticket issued");
        this.pnr = pnr;
        this.ticketNumber = ticketNumber;
    }

    public void expire() {
        moveTo(BookingStatus.EXPIRED, "Hold expired before payment");
    }

    public void cancel(String reason) {
        moveTo(BookingStatus.CANCELLED, reason);
    }

    /** The only place the status changes, so an illegal jump can never be persisted. */
    private void moveTo(BookingStatus next, String reason) {
        if (!status.canMoveTo(next)) {
            throw new IllegalStateException("Cannot move booking from " + status + " to " + next);
        }
        this.status = next;
        this.statusReason = reason;
    }

    public UUID getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public UUID getOfferId() { return offerId; }
    public String getPassengerName() { return passengerName; }
    public String getPassengerEmail() { return passengerEmail; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getCurrency() { return currency; }
    public BookingStatus getStatus() { return status; }
    public Instant getHoldExpiresAt() { return holdExpiresAt; }
    public String getPnr() { return pnr; }
    public String getTicketNumber() { return ticketNumber; }
    public String getStatusReason() { return statusReason; }
    public Instant getCreatedAt() { return createdAt; }
}
