package com.dke.insurance.policy_administration.domain;

/** Policy lifecycle. Legal moves are declared here so the domain class can enforce them. */
public enum PolicyStatus {
    ACTIVE,
    CANCELLED,
    EXPIRED;

    /** ACTIVE may end by cancellation or expiry; both end states are final. */
    public boolean canMoveTo(PolicyStatus target) {
        return this == ACTIVE && (target == CANCELLED || target == EXPIRED);
    }
}
