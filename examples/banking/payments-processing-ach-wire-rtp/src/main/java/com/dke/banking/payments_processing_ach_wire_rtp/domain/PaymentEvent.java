package com.dke.banking.payments_processing_ach_wire_rtp.domain;

import java.math.BigDecimal;
import java.util.UUID;

/** Domain events; topic() is the Apache Kafka topic each one would be published to. */
public sealed interface PaymentEvent {

    UUID paymentId();

    String topic();

    record PaymentSubmitted(UUID paymentId, Rail rail, BigDecimal amount) implements PaymentEvent {
        public String topic() { return "payments.submitted"; }
    }

    record PaymentSettled(UUID paymentId, Rail rail, BigDecimal amount) implements PaymentEvent {
        public String topic() { return "payments.settled"; }
    }

    record PaymentRejected(UUID paymentId, String reason) implements PaymentEvent {
        public String topic() { return "payments.rejected"; }
    }

    record PaymentReturned(UUID paymentId, String reason) implements PaymentEvent {
        public String topic() { return "payments.returned"; }
    }
}
