package com.dke.data_engineering.change_data_capture_cdc_pipelines.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** One execution of the CDC pipeline, with the metrics operators look at. */
@Entity
@Table(name = "pipeline_run")
public class PipelineRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RunStatus status;

    @Column(name = "triggered_by", nullable = false)
    private String triggeredBy;

    private Instant startedAt;
    private Instant finishedAt;
    private int eventsRead;
    private int applied;
    private int skippedStale;
    private int rejected;
    private String errorMessage;

    protected PipelineRun() {
    }

    public static PipelineRun pending(String source, String triggeredBy) {
        PipelineRun run = new PipelineRun();
        run.source = source;
        run.triggeredBy = triggeredBy;
        run.status = RunStatus.PENDING;
        return run;
    }

    public void start() {
        moveTo(RunStatus.RUNNING);
        startedAt = Instant.now();
    }

    public void succeed(int read, int applied, int skippedStale, int rejected) {
        moveTo(RunStatus.SUCCEEDED);
        this.eventsRead = read;
        this.applied = applied;
        this.skippedStale = skippedStale;
        this.rejected = rejected;
        finishedAt = Instant.now();
    }

    public void fail(String message) {
        moveTo(RunStatus.FAILED);
        errorMessage = message;
        finishedAt = Instant.now();
    }

    private void moveTo(RunStatus next) {
        if (!status.canMoveTo(next)) {
            throw new IllegalStateException("Illegal run transition " + status + " -> " + next);
        }
        status = next;
    }

    public Long getId() { return id; }
    public String getSource() { return source; }
    public RunStatus getStatus() { return status; }
    public String getTriggeredBy() { return triggeredBy; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public int getEventsRead() { return eventsRead; }
    public int getApplied() { return applied; }
    public int getSkippedStale() { return skippedStale; }
    public int getRejected() { return rejected; }
    public String getErrorMessage() { return errorMessage; }
}
