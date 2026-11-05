package com.dke.healthcare.electronic_health_records_ehr.service;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
