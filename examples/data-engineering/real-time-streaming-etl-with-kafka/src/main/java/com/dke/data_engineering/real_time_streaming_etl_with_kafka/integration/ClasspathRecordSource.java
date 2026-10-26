package com.dke.data_engineering.real_time_streaming_etl_with_kafka.integration;

import com.dke.data_engineering.real_time_streaming_etl_with_kafka.service.RecordSource;
import com.dke.data_engineering.real_time_streaming_etl_with_kafka.service.UnknownSourceException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/** Reads sample-data/{source}.csv, skipping the header and blank lines. */
@Component
public class ClasspathRecordSource implements RecordSource {

    @Override
    public List<String> readLines(String source) {
        if (!source.matches("[A-Za-z0-9._-]+")) {
            throw new UnknownSourceException(source);
        }
        Resource file = new ClassPathResource("sample-data/" + source + ".csv");
        if (!file.exists()) {
            throw new UnknownSourceException(source);
        }
        try (var in = file.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .skip(1).filter(l -> !l.isBlank()).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
