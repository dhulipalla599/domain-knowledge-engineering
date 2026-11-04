package com.dke.logistics.shipment_booking.domain;

import java.util.UUID;

/** Domain events. Each record name maps to a Kafka topic in production (see topic()). */
public sealed interface BookingEvent {

    UUID bookingId();

    String topic();

    record BookingRequested(UUID bookingId, String reference, String customerId) implements BookingEvent {
        public String topic() { return "shipment.booking.requested"; }
    }

    record BookingConfirmed(UUID bookingId, String reference, String carrierConfirmation) implements BookingEvent {
        public String topic() { return "shipment.booking.confirmed"; }
    }

    record BookingCancelled(UUID bookingId, String reference) implements BookingEvent {
        public String topic() { return "shipment.booking.cancelled"; }
    }
}
