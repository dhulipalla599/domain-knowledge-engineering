package com.dke.insurance.billing_and_premium_collection.domain;

import java.util.Set;

/** Lifecycle of one premium installment. Legal moves live here so the entity can enforce them. */
public enum InstallmentStatus {
    BILLED,
    PARTIALLY_PAID,
    OVERDUE,
    PAID,
    DEFAULTED;

    public static final Set<InstallmentStatus> OPEN = Set.of(BILLED, PARTIALLY_PAID, OVERDUE);

    /** PAID and DEFAULTED are final; OVERDUE stays OVERDUE until fully paid or the grace period ends. */
    public boolean canMoveTo(InstallmentStatus target) {
        return switch (this) {
            case BILLED -> target == PARTIALLY_PAID || target == PAID || target == OVERDUE;
            case PARTIALLY_PAID -> target == PARTIALLY_PAID || target == PAID || target == OVERDUE;
            case OVERDUE -> target == OVERDUE || target == PAID || target == DEFAULTED;
            case PAID, DEFAULTED -> false;
        };
    }
}
