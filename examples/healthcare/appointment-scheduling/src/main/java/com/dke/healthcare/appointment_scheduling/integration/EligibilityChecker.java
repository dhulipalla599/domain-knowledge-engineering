package com.dke.healthcare.appointment_scheduling.integration;

/** Port to the payer's eligibility service (a real adapter would call an X12 270/271 or FHIR endpoint). */
public interface EligibilityChecker {

    boolean isEligible(String insuranceMemberId);
}
