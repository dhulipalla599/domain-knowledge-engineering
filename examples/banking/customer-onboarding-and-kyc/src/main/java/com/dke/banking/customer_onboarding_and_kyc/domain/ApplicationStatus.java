package com.dke.banking.customer_onboarding_and_kyc.domain;

import java.util.Set;

/** Lifecycle of an onboarding application. Each status lists where it may go next. */
public enum ApplicationStatus {
    APPROVED,
    REJECTED,
    IN_REVIEW,
    SCREENING,
    SUBMITTED;

    public Set<ApplicationStatus> next() {
        return switch (this) {
            case SUBMITTED -> Set.of(SCREENING, REJECTED);
            case SCREENING -> Set.of(APPROVED, IN_REVIEW, REJECTED);
            case IN_REVIEW -> Set.of(APPROVED, REJECTED);
            case APPROVED, REJECTED -> Set.of();
        };
    }

    public boolean canMoveTo(ApplicationStatus target) {
        return next().contains(target);
    }

    /** What the applicant sees. Reviews are never disclosed ("tipping off" rule). */
    public String customerFacing() {
        return switch (this) {
            case APPROVED -> "APPROVED";
            case REJECTED -> "DECLINED";
            case SUBMITTED, SCREENING, IN_REVIEW -> "IN_PROGRESS";
        };
    }
}
