package com.dke.healthcare.appointment_scheduling.integration;

import com.dke.healthcare.appointment_scheduling.domain.AppointmentEvents.AppointmentBooked;
import com.dke.healthcare.appointment_scheduling.domain.AppointmentEvents.AppointmentCancelled;
import com.dke.healthcare.appointment_scheduling.domain.AppointmentEvents.PatientCheckedIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Stands in for the reminder, waitlist and audit consumers of the Kafka topics. */
@Component
public class AppointmentEventListener {

    private static final Logger log = LoggerFactory.getLogger(AppointmentEventListener.class);

    @EventListener
    void onBooked(AppointmentBooked event) {
        log.info("[appointment.booked] schedule reminders for {} at {}", event.appointmentId(), event.startTime());
    }

    @EventListener
    void onCancelled(AppointmentCancelled event) {
        log.info("[appointment.cancelled] offer slot {} to waitlist (late={})", event.startTime(), event.lateCancellation());
    }

    @EventListener
    void onCheckedIn(PatientCheckedIn event) {
        log.info("[appointment.checked-in] notify clinic dashboard for {}", event.appointmentId());
    }
}
