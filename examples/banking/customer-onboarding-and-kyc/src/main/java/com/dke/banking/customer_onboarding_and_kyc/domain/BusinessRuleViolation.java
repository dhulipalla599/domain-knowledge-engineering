package com.dke.banking.customer_onboarding_and_kyc.domain;

/** Thrown when a request breaks a business rule. Mapped to HTTP 422. */
public class BusinessRuleViolation extends RuntimeException {

    private final String code;

    public BusinessRuleViolation(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
