package com.dke.airline.flight_shopping_and_booking.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/** expectedPrice is the fare the customer saw; the hold fails if the offer no longer sells at that price. */
public record HoldRequest(@NotNull UUID offerId, @NotBlank String passengerName, @NotBlank @Email String passengerEmail,
                          @NotNull @DecimalMin("0.01") BigDecimal expectedPrice) {
}
