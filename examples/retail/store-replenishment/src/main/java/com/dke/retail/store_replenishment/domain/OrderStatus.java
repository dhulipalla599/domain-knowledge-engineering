package com.dke.retail.store_replenishment.domain;

/** Lifecycle of a store replenishment order. */
public enum OrderStatus {
    PROPOSED,  // suggested by the replenishment run or keyed by the store, not yet sent to the DC
    RELEASED,  // sent to the distribution center; counts as on-order stock
    RECEIVED,  // checked in at the store's back door
    CANCELLED; // withdrawn before release

    public boolean canMoveTo(OrderStatus next) {
        return switch (this) {
            case PROPOSED -> next == RELEASED || next == CANCELLED;
            case RELEASED -> next == RECEIVED;
            case RECEIVED, CANCELLED -> false;
        };
    }
}
