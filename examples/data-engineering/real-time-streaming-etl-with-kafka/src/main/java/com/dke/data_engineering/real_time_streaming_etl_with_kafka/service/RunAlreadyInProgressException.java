package com.dke.data_engineering.real_time_streaming_etl_with_kafka.service;

public class RunAlreadyInProgressException extends RuntimeException {
    public RunAlreadyInProgressException(String source) {
        super("A run for source '" + source + "' is already in progress");
    }
}
