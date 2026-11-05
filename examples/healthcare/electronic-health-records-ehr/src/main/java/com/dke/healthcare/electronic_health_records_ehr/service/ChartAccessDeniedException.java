package com.dke.healthcare.electronic_health_records_ehr.service;

public class ChartAccessDeniedException extends RuntimeException {

    public ChartAccessDeniedException(String message) {
        super(message);
    }
}
