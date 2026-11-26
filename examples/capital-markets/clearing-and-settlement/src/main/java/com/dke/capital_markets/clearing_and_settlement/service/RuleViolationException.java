package com.dke.capital_markets.clearing_and_settlement.service;

/** A business rule rejected the request; {@code rule} is a stable code clients can switch on. */
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
