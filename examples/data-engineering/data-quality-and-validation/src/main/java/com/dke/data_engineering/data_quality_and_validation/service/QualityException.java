package com.dke.data_engineering.data_quality_and_validation.service;

import org.springframework.http.HttpStatus;

/** A business-rule violation that maps to an HTTP status. */
public class QualityException extends RuntimeException {

    private final HttpStatus status;

    public QualityException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() { return status; }
}
