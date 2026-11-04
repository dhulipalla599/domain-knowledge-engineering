package com.dke.data_engineering.real_time_streaming_etl_with_kafka.service;

public class UnknownSourceException extends RuntimeException {
    public UnknownSourceException(String source) {
        super("Unknown source: " + source);
    }
}
