package com.dke.insurance.underwriting.service;

import org.springframework.http.HttpStatus;

/** A business rule rejected the request; the API layer turns it into a ProblemDetail. */
public class RuleViolationException extends RuntimeException {

    private final HttpStatus status;

    public RuleViolationException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
