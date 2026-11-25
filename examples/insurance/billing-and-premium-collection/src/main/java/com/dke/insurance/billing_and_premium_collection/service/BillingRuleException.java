package com.dke.insurance.billing_and_premium_collection.service;

import org.springframework.http.HttpStatus;

/** A business rule rejected the request; carries the HTTP status the API should answer with. */
public class BillingRuleException extends RuntimeException {
    private final HttpStatus status;

    public BillingRuleException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
