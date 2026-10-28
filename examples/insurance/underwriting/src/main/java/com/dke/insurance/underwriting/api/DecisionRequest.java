package com.dke.insurance.underwriting.api;

import jakarta.validation.constraints.NotBlank;

public record DecisionRequest(@NotBlank String underwriterId, boolean approve, @NotBlank String reason) {
}
