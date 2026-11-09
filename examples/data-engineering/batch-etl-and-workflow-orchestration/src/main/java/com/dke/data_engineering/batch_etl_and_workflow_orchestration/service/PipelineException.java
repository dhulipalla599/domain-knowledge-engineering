package com.dke.data_engineering.batch_etl_and_workflow_orchestration.service;

import org.springframework.http.HttpStatus;

/** A business-rule violation that maps to an HTTP status. */
public class PipelineException extends RuntimeException {

    private final HttpStatus status;

    public PipelineException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
