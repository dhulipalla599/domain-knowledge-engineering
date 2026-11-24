package com.dke.data_engineering.data_quality_and_validation.integration;

import com.dke.data_engineering.data_quality_and_validation.domain.RawClaim;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Extract port: where the daily claims file lands. Production adapter reads Amazon S3. */
public interface BatchSource {

    record SourceFile(String fileName, String checksum, List<String> header, List<RawClaim> rows) { }

    Optional<SourceFile> read(LocalDate businessDate);
}
