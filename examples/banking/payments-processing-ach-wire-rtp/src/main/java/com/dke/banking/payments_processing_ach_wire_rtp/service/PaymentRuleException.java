package com.dke.banking.payments_processing_ach_wire_rtp.service;

import org.springframework.http.HttpStatus;

/** A business rule was violated; carries the HTTP status the API should answer with. */
public class PaymentRuleException extends RuntimeException {

    private final HttpStatus status;

    public PaymentRuleException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
