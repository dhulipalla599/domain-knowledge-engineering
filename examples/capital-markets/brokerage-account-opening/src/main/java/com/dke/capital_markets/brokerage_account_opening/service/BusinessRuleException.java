package com.dke.capital_markets.brokerage_account_opening.service;

/** A request that is well-formed but breaks a business rule (maps to HTTP 422). */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
