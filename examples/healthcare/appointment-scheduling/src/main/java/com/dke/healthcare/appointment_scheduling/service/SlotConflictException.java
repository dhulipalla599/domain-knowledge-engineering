package com.dke.healthcare.appointment_scheduling.service;

/** The provider already has an active appointment in the requested window (maps to HTTP 409). */
public class SlotConflictException extends RuntimeException {

    public SlotConflictException(String message) {
        super(message);
    }
}
