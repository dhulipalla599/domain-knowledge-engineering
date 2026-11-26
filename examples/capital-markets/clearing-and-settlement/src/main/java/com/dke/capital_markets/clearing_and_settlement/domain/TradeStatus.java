package com.dke.capital_markets.clearing_and_settlement.domain;

import java.util.Set;

/** Lifecycle of a cleared trade (settlement obligation). */
public enum TradeStatus {
    PENDING_AFFIRMATION,
    AFFIRMED,
    SETTLED,
    FAILED,
    CANCELLED;

    public Set<TradeStatus> next() {
        return switch (this) {
            case PENDING_AFFIRMATION -> Set.of(AFFIRMED, CANCELLED);
            case AFFIRMED -> Set.of(SETTLED, FAILED, CANCELLED);
            case FAILED -> Set.of(SETTLED, FAILED, CANCELLED);
            case SETTLED, CANCELLED -> Set.of();
        };
    }

    /** Statuses that still represent an open obligation in the settlement run. */
    public boolean isSettleable() {
        return this == AFFIRMED || this == FAILED;
    }
}
