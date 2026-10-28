package com.dke.capital_markets.order_management_system.service;

/** A business rule rejected the request (mapped to HTTP 422). */
public class RuleViolationException extends RuntimeException {

    private final String rule;

    public RuleViolationException(String rule, String message) {
        super(message);
        this.rule = rule;
    }

    public String getRule() {
        return rule;
    }
}
