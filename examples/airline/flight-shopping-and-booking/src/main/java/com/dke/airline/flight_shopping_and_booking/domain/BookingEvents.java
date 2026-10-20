package com.dke.airline.flight_shopping_and_booking.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Domain events. Each record maps to a Kafka topic in production; the topic name is on the constant. */
public final class BookingEvents {

    public static final String HELD_TOPIC = "flight.booking.held";
    public static final String TICKETED_TOPIC = "flight.booking.ticketed";
    public static final String RELEASED_TOPIC = "flight.booking.released";

    private BookingEvents() {
    }

    public record BookingHeld(UUID bookingId, UUID offerId, BigDecimal amount, String currency, Instant occurredAt) {
    }

    public record BookingTicketed(UUID bookingId, String pnr, String ticketNumber, Instant occurredAt) {
    }

    /** Published when a hold expires or a booking is cancelled, so inventory and notifications stay in step. */
    public record BookingReleased(UUID bookingId, BookingStatus finalStatus, String reason, Instant occurredAt) {
    }
}
