package com.dke.data_engineering.change_data_capture_cdc_pipelines.api;

import com.dke.data_engineering.change_data_capture_cdc_pipelines.domain.PipelineRun;
import com.dke.data_engineering.change_data_capture_cdc_pipelines.domain.RunStatus;
import java.time.Instant;

public record RunResponse(Long id, String source, RunStatus status, String triggeredBy, Instant startedAt,
                          Instant finishedAt, int eventsRead, int applied, int skippedStale, int rejected,
                          String errorMessage) {

    public static RunResponse from(PipelineRun r) {
        return new RunResponse(r.getId(), r.getSource(), r.getStatus(), r.getTriggeredBy(), r.getStartedAt(),
                r.getFinishedAt(), r.getEventsRead(), r.getApplied(), r.getSkippedStale(), r.getRejected(),
                r.getErrorMessage());
    }
}
