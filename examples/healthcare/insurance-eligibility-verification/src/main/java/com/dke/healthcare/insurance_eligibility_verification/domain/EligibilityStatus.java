package com.dke.healthcare.insurance_eligibility_verification.domain;

import java.util.Set;

/** Lifecycle of one eligibility check (one X12 270 inquiry and its 271 answer). */
public enum EligibilityStatus {
    PENDING,      // inquiry created, waiting for the payer
    ACTIVE,       // payer confirmed coverage on the date of service
    INACTIVE,     // payer answered, but there is no coverage on the date of service
    REJECTED,     // payer could not process the inquiry (AAA reject, e.g. member not found)
    PAYER_ERROR;  // payer or clearinghouse unreachable; safe to retry

    public Set<EligibilityStatus> allowedNext() {
        return switch (this) {
            case PENDING -> Set.of(ACTIVE, INACTIVE, REJECTED, PAYER_ERROR);
            case PAYER_ERROR -> Set.of(PENDING);
            case ACTIVE, INACTIVE, REJECTED -> Set.of();
        };
    }

    public boolean canMoveTo(EligibilityStatus next) {
        return allowedNext().contains(next);
    }
}
