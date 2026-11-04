package com.dke.capital_markets.order_management_system.domain;

import java.util.Set;

/** Order lifecycle. Legal moves are declared here so no caller can skip a step. */
public enum OrderStatus {
    NEW, ACCEPTED, PARTIALLY_FILLED, FILLED, CANCELED, REJECTED;

    public Set<OrderStatus> next() {
        return switch (this) {
            case NEW -> Set.of(ACCEPTED, REJECTED);
            case ACCEPTED -> Set.of(PARTIALLY_FILLED, FILLED, CANCELED);
            case PARTIALLY_FILLED -> Set.of(PARTIALLY_FILLED, FILLED, CANCELED);
            case FILLED, CANCELED, REJECTED -> Set.of();
        };
    }

    public boolean isOpen() {
        return this == ACCEPTED || this == PARTIALLY_FILLED;
    }
}
