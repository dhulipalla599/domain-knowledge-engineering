package com.dke.insurance.underwriting.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record SubmitApplicationRequest(
        @NotBlank String applicantName,
        @NotNull @Positive BigDecimal sumInsured,
        @Min(1800) @Max(2100) int yearBuilt,
        boolean floodZone) {
}
