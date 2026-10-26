package com.dke.data_engineering.real_time_streaming_etl_with_kafka.service;

import java.util.List;

/** Extract port. Production: a Kafka consumer on orders.raw; here a file on the classpath. */
public interface RecordSource {

    /** @throws UnknownSourceException if the source does not exist */
    List<String> readLines(String source);
}
