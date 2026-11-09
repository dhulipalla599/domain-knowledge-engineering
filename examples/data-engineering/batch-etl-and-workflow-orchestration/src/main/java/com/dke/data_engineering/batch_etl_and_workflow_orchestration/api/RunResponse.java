package com.dke.data_engineering.batch_etl_and_workflow_orchestration.api;

import com.dke.data_engineering.batch_etl_and_workflow_orchestration.domain.PipelineRun;
import com.dke.data_engineering.batch_etl_and_workflow_orchestration.domain.RunStatus;
import java.time.Instant;
import java.time.LocalDate;

public record RunResponse(Long id, LocalDate businessDate, RunStatus status, String triggeredBy,
                          int rowsExtracted, int rowsLoaded, int rowsRejected,
                          String failureReason, Instant startedAt, Instant finishedAt) {

    public static RunResponse from(PipelineRun r) {
        return new RunResponse(r.getId(), r.getBusinessDate(), r.getStatus(), r.getTriggeredBy().name(),
                r.getRowsExtracted(), r.getRowsLoaded(), r.getRowsRejected(),
                r.getFailureReason(), r.getStartedAt(), r.getFinishedAt());
    }
}
