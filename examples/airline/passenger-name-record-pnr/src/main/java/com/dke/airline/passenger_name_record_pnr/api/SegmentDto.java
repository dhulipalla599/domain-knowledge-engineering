package com.dke.airline.passenger_name_record_pnr.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record SegmentDto(@NotBlank String flightNumber, @NotNull LocalDate departureDate,
                         @NotBlank String origin, @NotBlank String destination) {
}
