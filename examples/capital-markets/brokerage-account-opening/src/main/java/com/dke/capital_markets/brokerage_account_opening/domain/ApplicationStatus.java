package com.dke.capital_markets.brokerage_account_opening.domain;

import java.util.EnumSet;
import java.util.Set;

/** Lifecycle of an account application. Terminal states have no outgoing transitions. */
public enum ApplicationStatus {
    SUBMITTED,
    PENDING_REVIEW,
    APPROVED,
    REJECTED,
    ACTIVE;

    public Set<ApplicationStatus> allowedNext() {
        return switch (this) {
            case SUBMITTED -> EnumSet.of(PENDING_REVIEW, APPROVED, REJECTED);
            case PENDING_REVIEW -> EnumSet.of(APPROVED, REJECTED);
            case APPROVED -> EnumSet.of(ACTIVE);
            case REJECTED, ACTIVE -> EnumSet.noneOf(ApplicationStatus.class);
        };
    }

    public boolean canMoveTo(ApplicationStatus next) {
        return allowedNext().contains(next);
    }
}
