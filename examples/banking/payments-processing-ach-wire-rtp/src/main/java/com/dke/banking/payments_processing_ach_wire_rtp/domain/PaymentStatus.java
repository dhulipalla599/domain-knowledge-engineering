package com.dke.banking.payments_processing_ach_wire_rtp.domain;

import java.util.EnumSet;
import java.util.Set;

/** Lifecycle of a payment order; the allowed moves live here so they cannot drift. */
public enum PaymentStatus {
    RECEIVED, VALIDATED, SUBMITTED, SETTLED, REJECTED, RETURNED;

    public Set<PaymentStatus> allowedNext() {
        return switch (this) {
            case RECEIVED -> EnumSet.of(VALIDATED, REJECTED);
            case VALIDATED -> EnumSet.of(SUBMITTED);
            case SUBMITTED -> EnumSet.of(SETTLED, REJECTED);
            case SETTLED -> EnumSet.of(RETURNED);
            case REJECTED, RETURNED -> EnumSet.noneOf(PaymentStatus.class);
        };
    }
}
