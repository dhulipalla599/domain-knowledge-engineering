package com.dke.telecom.number_porting.domain;

/** Lifecycle of a port request. COMPLETED, REJECTED and CANCELLED are terminal. */
public enum PortStatus {
    SUBMITTED, VALIDATED, SCHEDULED, COMPLETED, REJECTED, CANCELLED;

    public boolean canMoveTo(PortStatus target) {
        return switch (this) {
            case SUBMITTED -> target == VALIDATED || target == REJECTED || target == CANCELLED;
            case VALIDATED -> target == SCHEDULED || target == CANCELLED;
            case SCHEDULED -> target == COMPLETED || target == CANCELLED;
            case COMPLETED, REJECTED, CANCELLED -> false;
        };
    }
}
