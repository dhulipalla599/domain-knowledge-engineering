package com.dke.insurance.policy_administration.api;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record EndorsementRequest(
        @NotNull @Positive BigDecimal newSumInsured,
        @NotNull LocalDate effectiveDate,
        @NotBlank String reason) {
}
