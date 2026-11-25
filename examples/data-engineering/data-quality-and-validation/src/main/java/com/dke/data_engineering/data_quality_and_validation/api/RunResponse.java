package com.dke.data_engineering.data_quality_and_validation.api;

import com.dke.data_engineering.data_quality_and_validation.domain.RunStatus;
import com.dke.data_engineering.data_quality_and_validation.domain.ValidationRun;
import java.time.Instant;
import java.time.LocalDate;

public record RunResponse(Long id, String fileName, LocalDate businessDate, RunStatus status,
                          int rowsReceived, int rowsLoaded, int rowsRejected, int rowsWarned,
                          double errorRatio, String statusReason, String releasedBy,
                          Instant receivedAt, Instant finishedAt) {

    public static RunResponse from(ValidationRun r) {
        return new RunResponse(r.getId(), r.getFileName(), r.getBusinessDate(), r.getStatus(),
                r.getRowsReceived(), r.getRowsLoaded(), r.getRowsRejected(), r.getRowsWarned(),
                Math.round(r.getErrorRatio() * 10000) / 10000.0, r.getStatusReason(), r.getReleasedBy(),
                r.getReceivedAt(), r.getFinishedAt());
    }
}
