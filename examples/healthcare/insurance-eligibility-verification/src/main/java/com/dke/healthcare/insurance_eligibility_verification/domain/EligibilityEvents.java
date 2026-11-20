package com.dke.healthcare.insurance_eligibility_verification.domain;

import java.time.LocalDate;

/** Domain events, named after the Kafka topics they stand in for. */
public final class EligibilityEvents {

    private EligibilityEvents() {
    }

    /** Topic {@code eligibility.verified}. */
    public record EligibilityVerified(String checkId, String coverageId, LocalDate serviceDate, String serviceTypeCode) {
    }

    /** Topic {@code eligibility.coverage-inactive}. */
    public record CoverageInactive(String checkId, String coverageId, LocalDate serviceDate, String reason) {
    }

    /** Topic {@code eligibility.check-failed}; status is REJECTED or PAYER_ERROR. */
    public record EligibilityCheckFailed(String checkId, String coverageId, EligibilityStatus status, String reason) {
    }
}
