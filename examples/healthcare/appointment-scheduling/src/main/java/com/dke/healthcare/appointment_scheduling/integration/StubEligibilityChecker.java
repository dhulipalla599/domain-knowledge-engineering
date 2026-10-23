package com.dke.healthcare.appointment_scheduling.integration;

import org.springframework.stereotype.Component;

/** Deterministic stand-in: members whose id starts with "LAPSED" have no active coverage. */
@Component
public class StubEligibilityChecker implements EligibilityChecker {

    @Override
    public boolean isEligible(String insuranceMemberId) {
        return insuranceMemberId != null && !insuranceMemberId.startsWith("LAPSED");
    }
}
