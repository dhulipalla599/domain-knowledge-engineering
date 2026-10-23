package com.dke.healthcare.appointment_scheduling.service;

/** A business rule rejected the request (maps to HTTP 422). */
public class BookingRuleException extends RuntimeException {

    public BookingRuleException(String message) {
        super(message);
    }
}
