package com.dke.insurance.policy_administration.api;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** Body for cancellation (effective date) and expiry (as-of date). */
public record DateRequest(@NotNull LocalDate effectiveDate) {
}
