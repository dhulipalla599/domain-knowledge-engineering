package com.dke.healthcare.insurance_eligibility_verification.service;

/** A business rule rejected the request; reported as 422 Unprocessable Entity. */
public class EligibilityRuleException extends RuntimeException {
    public EligibilityRuleException(String message) {
        super(message);
    }
}
