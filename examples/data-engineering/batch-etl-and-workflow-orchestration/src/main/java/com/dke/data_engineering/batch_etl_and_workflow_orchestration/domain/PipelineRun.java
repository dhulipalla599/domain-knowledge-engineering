package com.dke.data_engineering.batch_etl_and_workflow_orchestration.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

/** One execution of the daily sales load for a business date. Also stores the run metrics. */
@Entity
@Table(name = "pipeline_run")
public class PipelineRun {

    public enum Trigger { API, SCHEDULE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate businessDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RunStatus status = RunStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Trigger triggeredBy;

    private int rowsExtracted;
    private int rowsLoaded;
    private int rowsRejected;
    private String failureReason;
    private Instant startedAt;
    private Instant finishedAt;

    protected PipelineRun() { }

    public PipelineRun(LocalDate businessDate, Trigger triggeredBy) {
        this.businessDate = businessDate;
        this.triggeredBy = triggeredBy;
        this.startedAt = Instant.now();
    }

    /** Enforces the legal lifecycle; terminal states stamp the finish time. */
    public void transitionTo(RunStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new IllegalStateException("Illegal run transition " + status + " -> " + next);
        }
        status = next;
        if (next == RunStatus.SUCCEEDED || next == RunStatus.FAILED) {
            finishedAt = Instant.now();
        }
    }

    public void recordExtract(int extracted) { this.rowsExtracted = extracted; }

    public void recordTransform(int rejected) { this.rowsRejected = rejected; }

    public void recordLoad(int loaded) { this.rowsLoaded = loaded; }

    public void fail(String reason) {
        this.failureReason = reason;
        transitionTo(RunStatus.FAILED);
    }

    public Long getId() { return id; }
    public LocalDate getBusinessDate() { return businessDate; }
    public RunStatus getStatus() { return status; }
    public Trigger getTriggeredBy() { return triggeredBy; }
    public int getRowsExtracted() { return rowsExtracted; }
    public int getRowsLoaded() { return rowsLoaded; }
    public int getRowsRejected() { return rowsRejected; }
    public String getFailureReason() { return failureReason; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
}
