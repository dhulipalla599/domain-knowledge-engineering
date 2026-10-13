package com.dke.insurance.quote_and_rating.api;

import com.dke.insurance.quote_and_rating.domain.VehicleType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateQuoteRequest(
        @NotBlank String driverName,
        @Min(0) @Max(120) int driverAge,
        @NotNull @Pattern(regexp = "[A-Z]{2}", message = "must be a two-letter state code") String state,
        @NotNull VehicleType vehicleType,
        @Min(0) int priorClaims) {
}
