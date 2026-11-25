package com.dke.data_engineering.data_quality_and_validation.domain;

import java.time.LocalDate;

/** Domain events. Each stands in for an Apache Kafka topic named in the comment. */
public final class QualityEvents {

    private QualityEvents() { }

    /** Topic {@code dq.batch.loaded}: the batch passed the gate (or was released) and is in the trusted table. */
    public record BatchLoaded(Long runId, String fileName, LocalDate businessDate, int rowsLoaded,
                              int rowsRejected, boolean released) { }

    /** Topic {@code dq.batch.quarantined}: the batch breached the quality gate; nothing was loaded. */
    public record BatchQuarantined(Long runId, String fileName, LocalDate businessDate,
                                   double errorRatio, String reason) { }

    /** Topic {@code dq.batch.failed}: the file broke the schema contract and cannot be released. */
    public record BatchFailed(Long runId, String fileName, String reason) { }
}
