package com.dke.healthcare.appointment_scheduling.domain;

import java.time.Instant;
import java.util.UUID;

/** Domain events; each stands in for a Kafka topic named in its Javadoc. */
public final class AppointmentEvents {

    private AppointmentEvents() {
    }

    /** Topic: appointment.booked */
    public record AppointmentBooked(UUID appointmentId, String patientId, String providerId, Instant startTime) {
    }

    /** Topic: appointment.cancelled */
    public record AppointmentCancelled(UUID appointmentId, String providerId, Instant startTime, boolean lateCancellation) {
    }

    /** Topic: appointment.checked-in */
    public record PatientCheckedIn(UUID appointmentId, String providerId) {
    }
}
