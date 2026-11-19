package com.dke.banking.card_authorization_and_settlement.service;

import org.springframework.http.HttpStatus;

/** A business rule rejected the request; carries the HTTP status the API should return. */
public class CardRuleException extends RuntimeException {

    private final HttpStatus status;

    public CardRuleException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
