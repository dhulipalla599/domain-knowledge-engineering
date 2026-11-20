package com.dke.healthcare.insurance_eligibility_verification.service;

/** Reported as 404 Not Found. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
