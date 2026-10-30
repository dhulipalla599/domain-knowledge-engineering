package com.dke.retail.inventory_management.domain;

import java.util.Set;

/** Lifecycle of a stock reservation. PENDING is the only state with outgoing transitions. */
public enum ReservationStatus {
    PENDING, CONFIRMED, RELEASED, EXPIRED;

    public Set<ReservationStatus> allowedNext() {
        return this == PENDING ? Set.of(CONFIRMED, RELEASED, EXPIRED) : Set.of();
    }
}
