package com.dke.insurance.underwriting.domain;

/** Lifecycle of a policy application. Legal moves are declared here, enforced in {@link PolicyApplication}. */
public enum ApplicationStatus {
    SUBMITTED, REFERRED, APPROVED, DECLINED;

    public boolean canMoveTo(ApplicationStatus next) {
        return switch (this) {
            case SUBMITTED -> next == APPROVED || next == REFERRED || next == DECLINED;
            case REFERRED -> next == APPROVED || next == DECLINED;
            case APPROVED, DECLINED -> false;
        };
    }
}
