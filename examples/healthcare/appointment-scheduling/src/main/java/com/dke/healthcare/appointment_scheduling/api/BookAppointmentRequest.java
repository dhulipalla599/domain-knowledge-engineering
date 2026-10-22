package com.dke.healthcare.appointment_scheduling.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record BookAppointmentRequest(
        @NotBlank String patientId,
        @NotBlank String providerId,
        @NotNull Instant startTime,
        String reason) {
}
