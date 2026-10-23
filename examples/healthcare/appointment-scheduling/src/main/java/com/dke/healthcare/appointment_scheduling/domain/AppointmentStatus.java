package com.dke.healthcare.appointment_scheduling.domain;

import java.util.Set;

public enum AppointmentStatus {
    BOOKED, CHECKED_IN, COMPLETED, CANCELLED, NO_SHOW;

    public Set<AppointmentStatus> allowedNext() {
        return switch (this) {
            case BOOKED -> Set.of(CHECKED_IN, CANCELLED, NO_SHOW);
            case CHECKED_IN -> Set.of(COMPLETED);
            case COMPLETED, CANCELLED, NO_SHOW -> Set.of();
        };
    }

    /** Statuses that still occupy the provider's calendar. */
    public static final Set<AppointmentStatus> HOLDING_SLOT = Set.of(BOOKED, CHECKED_IN);
}
