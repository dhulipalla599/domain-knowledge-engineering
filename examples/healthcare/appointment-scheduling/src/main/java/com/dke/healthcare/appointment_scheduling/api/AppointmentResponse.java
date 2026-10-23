package com.dke.healthcare.appointment_scheduling.api;

import com.dke.healthcare.appointment_scheduling.domain.Appointment;
import com.dke.healthcare.appointment_scheduling.domain.AppointmentStatus;
import java.time.Instant;
import java.util.UUID;

public record AppointmentResponse(UUID id, String patientId, String providerId, Instant startTime,
                                  Instant endTime, AppointmentStatus status, String reason) {

    static AppointmentResponse from(Appointment a) {
        return new AppointmentResponse(a.getId(), a.getPatientId(), a.getProviderId(), a.getStartTime(),
                a.getEndTime(), a.getStatus(), a.getReason());
    }
}
