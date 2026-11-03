package com.dke.airline.passenger_name_record_pnr.domain;

import java.util.EnumSet;
import java.util.Set;

/** Lifecycle of a PNR. TICKETED PNRs can still be cancelled (a refund would follow in production). */
public enum PnrStatus {
    ON_HOLD, TICKETED, CANCELLED;

    public Set<PnrStatus> allowedNext() {
        return switch (this) {
            case ON_HOLD -> EnumSet.of(TICKETED, CANCELLED);
            case TICKETED -> EnumSet.of(CANCELLED);
            case CANCELLED -> EnumSet.noneOf(PnrStatus.class);
        };
    }
}
