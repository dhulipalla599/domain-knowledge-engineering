package com.dke.airline.flight_shopping_and_booking.domain;

import java.util.Set;

/** Booking lifecycle. HELD is the only state that owns inventory without a ticket behind it. */
public enum BookingStatus {
    HELD, CONFIRMED, TICKETED, EXPIRED, CANCELLED;

    public boolean canMoveTo(BookingStatus next) {
        return allowedNext().contains(next);
    }

    private Set<BookingStatus> allowedNext() {
        return switch (this) {
            case HELD -> Set.of(CONFIRMED, EXPIRED, CANCELLED);
            case CONFIRMED -> Set.of(TICKETED, CANCELLED);
            case TICKETED -> Set.of(CANCELLED);
            case EXPIRED, CANCELLED -> Set.of();
        };
    }
}
