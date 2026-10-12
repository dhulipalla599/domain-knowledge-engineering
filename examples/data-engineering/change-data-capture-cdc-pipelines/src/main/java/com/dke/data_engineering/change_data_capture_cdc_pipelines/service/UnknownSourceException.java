package com.dke.data_engineering.change_data_capture_cdc_pipelines.service;

public class UnknownSourceException extends RuntimeException {

    public UnknownSourceException(String name) {
        super("No change log named '" + name + "'");
    }
}
