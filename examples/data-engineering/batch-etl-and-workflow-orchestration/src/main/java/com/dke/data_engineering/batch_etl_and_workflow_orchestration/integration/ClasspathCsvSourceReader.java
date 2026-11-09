package com.dke.data_engineering.batch_etl_and_workflow_orchestration.integration;

import com.dke.data_engineering.batch_etl_and_workflow_orchestration.domain.RawSale;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Reads sample-data/sales-YYYY-MM-DD.csv from the classpath. Stand-in for an S3 landing bucket. */
@Component
public class ClasspathCsvSourceReader implements SourceReader {

    private static ClassPathResource file(LocalDate date) {
        return new ClassPathResource("sample-data/sales-" + date + ".csv");
    }

    @Override
    public boolean exists(LocalDate businessDate) {
        return file(businessDate).exists();
    }

    @Override
    public List<RawSale> read(LocalDate businessDate) {
        try (var reader = new BufferedReader(
                new InputStreamReader(file(businessDate).getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().skip(1).filter(l -> !l.isBlank()).map(ClasspathCsvSourceReader::parse).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static RawSale parse(String line) {
        String[] c = (line + ",,,,,").split(",", -1);
        return new RawSale(c[0].trim(), c[1].trim(), c[2].trim(), c[3].trim(), c[4].trim(), c[5].trim());
    }
}
