package com.dke.airline.flight_shopping_and_booking.api;

import jakarta.validation.constraints.NotBlank;

public record CancelRequest(@NotBlank String reason) {
}
