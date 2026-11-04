package com.dke.insurance.quote_and_rating.domain;

import java.util.EnumSet;
import java.util.Set;

/** Lifecycle of a quote. BOUND and EXPIRED are terminal. */
public enum QuoteStatus {
    DRAFT, RATED, BOUND, EXPIRED;

    public Set<QuoteStatus> allowedNext() {
        return switch (this) {
            case DRAFT -> EnumSet.of(RATED);
            case RATED -> EnumSet.of(BOUND, EXPIRED);
            case BOUND, EXPIRED -> EnumSet.noneOf(QuoteStatus.class);
        };
    }

    public boolean canMoveTo(QuoteStatus next) {
        return allowedNext().contains(next);
    }
}
