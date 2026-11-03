package com.dke.airline.passenger_name_record_pnr.api;

import jakarta.validation.constraints.NotBlank;

public record PassengerDto(@NotBlank String firstName, @NotBlank String lastName) {
}
