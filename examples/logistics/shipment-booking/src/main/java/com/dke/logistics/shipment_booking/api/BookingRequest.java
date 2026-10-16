package com.dke.logistics.shipment_booking.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record BookingRequest(
        @NotBlank String customerId,
        @NotBlank String quoteId,
        @Positive double weightKg,
        String unNumber) {
}
