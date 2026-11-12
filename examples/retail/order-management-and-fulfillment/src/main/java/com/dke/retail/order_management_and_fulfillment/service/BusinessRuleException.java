package com.dke.retail.order_management_and_fulfillment.service;

import org.springframework.http.HttpStatus;

/** A business rule rejected the request. {@code rule} is a stable code clients can switch on. */
public class BusinessRuleException extends RuntimeException {

    private final String rule;
    private final HttpStatus status;

    public BusinessRuleException(String rule, String message, HttpStatus status) {
        super(message);
        this.rule = rule;
        this.status = status;
    }

    public String getRule() { return rule; }
    public HttpStatus getStatus() { return status; }
}
