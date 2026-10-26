package com.dke.data_engineering.real_time_streaming_etl_with_kafka.api;

import com.dke.data_engineering.real_time_streaming_etl_with_kafka.domain.PipelineRun;
import com.dke.data_engineering.real_time_streaming_etl_with_kafka.domain.RunStatus;

public record RunResponse(Long runId, String source, RunStatus status, int recordsRead,
                          int recordsLoaded, int duplicatesSkipped, int recordsRejected) {

    public static RunResponse from(PipelineRun r) {
        return new RunResponse(r.getId(), r.getSource(), r.getStatus(), r.getRecordsRead(),
                r.getRecordsLoaded(), r.getDuplicatesSkipped(), r.getRecordsRejected());
    }
}
