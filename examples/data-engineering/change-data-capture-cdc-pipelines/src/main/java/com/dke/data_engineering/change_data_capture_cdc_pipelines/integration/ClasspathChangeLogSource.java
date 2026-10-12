package com.dke.data_engineering.change_data_capture_cdc_pipelines.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dke.data_engineering.change_data_capture_cdc_pipelines.domain.ChangeEvent;
import com.dke.data_engineering.change_data_capture_cdc_pipelines.service.ChangeLogSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Reads sample-data/{name}.json from the classpath. Production would consume Debezium events from Kafka. */
@Component
public class ClasspathChangeLogSource implements ChangeLogSource {

    private final ObjectMapper mapper;

    public ClasspathChangeLogSource(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean exists(String name) {
        return new ClassPathResource(path(name)).exists();
    }

    @Override
    public List<ChangeEvent> read(String name) {
        try (InputStream in = new ClassPathResource(path(name)).getInputStream()) {
            return mapper.readValue(in, new TypeReference<List<ChangeEvent>>() { });
        } catch (IOException ex) {
            throw new UncheckedIOException("Cannot read change log " + name, ex);
        }
    }

    private static String path(String name) {
        return "sample-data/" + name + ".json";
    }
}
