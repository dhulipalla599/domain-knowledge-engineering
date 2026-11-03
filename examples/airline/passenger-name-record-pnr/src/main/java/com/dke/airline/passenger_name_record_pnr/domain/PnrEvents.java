package com.dke.airline.passenger_name_record_pnr.domain;

import java.time.Instant;

/** Domain events. Each maps to a Kafka topic in production (named in the comment). */
public final class PnrEvents {

    private PnrEvents() {
    }

    /** topic: pnr.created */
    public record PnrCreated(String recordLocator, int passengerCount, Instant ticketTimeLimit) {
    }

    /** topic: pnr.ticketed */
    public record PnrTicketed(String recordLocator, java.util.List<String> ticketNumbers) {
    }

    /** topic: pnr.cancelled */
    public record PnrCancelled(String recordLocator, String reason) {
    }
}
