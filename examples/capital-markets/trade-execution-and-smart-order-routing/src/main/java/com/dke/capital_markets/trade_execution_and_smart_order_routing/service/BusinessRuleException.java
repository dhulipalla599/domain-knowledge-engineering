package com.dke.capital_markets.trade_execution_and_smart_order_routing.service;

/** A pre-trade rule rejected the order. {@code rule} is a stable code clients can switch on. */
public class BusinessRuleException extends RuntimeException {

    private final String rule;
    private final boolean conflict;

    public BusinessRuleException(String rule, String message, boolean conflict) {
        super(message);
        this.rule = rule;
        this.conflict = conflict;
    }

    public String getRule() { return rule; }
    public boolean isConflict() { return conflict; }
}
