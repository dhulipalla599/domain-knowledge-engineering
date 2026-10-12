package com.dke.data_engineering.change_data_capture_cdc_pipelines.service;

import com.dke.data_engineering.change_data_capture_cdc_pipelines.domain.ChangeEvent;
import java.util.List;

/** Where change events come from. Production: a Debezium connector writing to Apache Kafka. Here: a JSON file. */
public interface ChangeLogSource {

    boolean exists(String name);

    List<ChangeEvent> read(String name);
}
