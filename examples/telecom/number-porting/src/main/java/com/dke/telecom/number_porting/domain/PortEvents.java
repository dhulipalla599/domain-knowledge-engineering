package com.dke.telecom.number_porting.domain;

import java.time.LocalDate;

/** Domain events. Each stands in for a Kafka topic named in the comment. */
public final class PortEvents {

    private PortEvents() {
    }

    /** topic: port.request.submitted */
    public record PortRequestSubmitted(Long requestId, String msisdn, LocalDate portDate) {}

    /** topic: port.request.validated */
    public record PortRequestValidated(Long requestId, String msisdn) {}

    /** topic: port.request.scheduled */
    public record PortRequestScheduled(Long requestId, String msisdn, LocalDate portDate) {}

    /** topic: port.request.rejected */
    public record PortRequestRejected(Long requestId, String msisdn, String reason) {}

    /** topic: port.request.cancelled */
    public record PortRequestCancelled(Long requestId, String msisdn) {}

    /** topic: port.request.completed */
    public record PortCompleted(Long requestId, String msisdn, String newOperator) {}
}
