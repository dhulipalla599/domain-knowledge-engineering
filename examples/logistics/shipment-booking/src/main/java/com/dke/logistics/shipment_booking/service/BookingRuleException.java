package com.dke.logistics.shipment_booking.service;

import org.springframework.http.HttpStatus;

/** A business rule rejected the request; carries the HTTP status to report. */
public class BookingRuleException extends RuntimeException {

    private final HttpStatus status;

    public BookingRuleException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
