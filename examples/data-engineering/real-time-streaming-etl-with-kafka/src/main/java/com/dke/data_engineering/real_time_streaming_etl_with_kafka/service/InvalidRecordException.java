package com.dke.data_engineering.real_time_streaming_etl_with_kafka.service;

public class InvalidRecordException extends RuntimeException {
    public InvalidRecordException(String reason) {
        super(reason);
    }
}
