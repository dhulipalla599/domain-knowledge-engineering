package com.dke.data_engineering.data_quality_and_validation.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

/** One validation run of a claims file, with its quality metrics. */
@Entity
@Table(name = "validation_run")
public class ValidationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false, length = 64)
    private String checksum;

    @Column(nullable = false)
    private LocalDate businessDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RunStatus status = RunStatus.RECEIVED;

    private int rowsReceived;
    private int rowsLoaded;
    private int rowsRejected;
    private int rowsWarned;
    private double errorRatio;
    private String statusReason;
    private String releasedBy;
    private Instant receivedAt;
    private Instant finishedAt;

    protected ValidationRun() { }

    public ValidationRun(String fileName, String checksum, LocalDate businessDate) {
        this.fileName = fileName;
        this.checksum = checksum;
        this.businessDate = businessDate;
        this.receivedAt = Instant.now();
    }

    /** Enforces the legal lifecycle; every state after VALIDATING stamps the finish time. */
    public void transitionTo(RunStatus next, String reason) {
        if (!status.canTransitionTo(next)) {
            throw new IllegalStateException("Illegal run transition " + status + " -> " + next);
        }
        this.status = next;
        this.statusReason = reason;
        if (next != RunStatus.VALIDATING) {
            this.finishedAt = Instant.now();
        }
    }

    public void recordMetrics(int received, int rejected, int warned) {
        this.rowsReceived = received;
        this.rowsRejected = rejected;
        this.rowsWarned = warned;
        this.errorRatio = received == 0 ? 0.0 : (double) rejected / received;
    }

    public void recordLoad(int loaded) { this.rowsLoaded = loaded; }

    public void release(String approvedBy, String reason, int loaded) {
        transitionTo(RunStatus.LOADED, "Released by " + approvedBy + ": " + reason);
        this.releasedBy = approvedBy;
        this.rowsLoaded = loaded;
    }

    public Long getId() { return id; }
    public String getFileName() { return fileName; }
    public String getChecksum() { return checksum; }
    public LocalDate getBusinessDate() { return businessDate; }
    public RunStatus getStatus() { return status; }
    public int getRowsReceived() { return rowsReceived; }
    public int getRowsLoaded() { return rowsLoaded; }
    public int getRowsRejected() { return rowsRejected; }
    public int getRowsWarned() { return rowsWarned; }
    public double getErrorRatio() { return errorRatio; }
    public String getStatusReason() { return statusReason; }
    public String getReleasedBy() { return releasedBy; }
    public Instant getReceivedAt() { return receivedAt; }
    public Instant getFinishedAt() { return finishedAt; }
}
