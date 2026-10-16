package com.dke.logistics.shipment_booking.domain;

import java.util.Set;

/** Lifecycle of a booking. Legal moves are declared here, enforced in {@link Booking}. */
public enum BookingStatus {
    PENDING, CONFIRMED, CANCELLED, DISPATCHED;

    public Set<BookingStatus> allowedNext() {
        return switch (this) {
            case PENDING -> Set.of(CONFIRMED, CANCELLED);
            case CONFIRMED -> Set.of(DISPATCHED, CANCELLED);
            case CANCELLED, DISPATCHED -> Set.of();
        };
    }
}
