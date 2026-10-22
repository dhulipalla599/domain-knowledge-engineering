package com.dke.banking.core_banking_ledger_and_accounts.domain;

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
