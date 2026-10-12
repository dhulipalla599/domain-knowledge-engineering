package com.dke.data_engineering.change_data_capture_cdc_pipelines.service;

public class RunAlreadyInProgressException extends RuntimeException {

    public RunAlreadyInProgressException() {
        super("A pipeline run is already in progress");
    }
}
