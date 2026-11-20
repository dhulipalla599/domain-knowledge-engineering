package com.dke.healthcare.insurance_eligibility_verification.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

/** serviceTypeCode is an X12 service type code, e.g. 30 (health benefit plan coverage) or 98 (office visit). */
public record EligibilityCheckRequest(
        @NotBlank String coverageId,
        @NotNull LocalDate serviceDate,
        @NotBlank @Pattern(regexp = "[A-Z0-9]{1,2}") String serviceTypeCode) {
}
