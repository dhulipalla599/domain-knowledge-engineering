package com.dke.insurance.quote_and_rating.domain;

import org.springframework.http.HttpStatus;

/** A business rule was broken; carries the HTTP status the API should answer with. */
public class QuoteRuleException extends RuntimeException {

    private final HttpStatus status;

    public QuoteRuleException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
