package com.dke.insurance.policy_administration.api;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record IssuePolicyRequest(
        @NotBlank String quoteId,
        @NotBlank String holderName,
        @NotNull @Positive BigDecimal sumInsured,
        @NotNull @Positive @DecimalMax("0.1") BigDecimal rate,
        @NotNull LocalDate inception) {
}
