package com.dke.data_engineering.real_time_streaming_etl_with_kafka.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** One execution of the extract-transform-validate-load pipeline, with its metrics. */
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
    private RunStatus status = RunStatus.RUNNING;

    private Instant startedAt = Instant.now();
    private Instant finishedAt;
    private int recordsRead;
    private int recordsLoaded;
    private int duplicatesSkipped;
    private int recordsRejected;

    protected PipelineRun() {
    }

    public PipelineRun(String source) {
        this.source = source;
    }

    public void complete(int read, int loaded, int duplicates, int rejected) {
        transitionTo(rejected > 0 ? RunStatus.COMPLETED_WITH_REJECTS : RunStatus.SUCCEEDED);
        this.recordsRead = read;
        this.recordsLoaded = loaded;
        this.duplicatesSkipped = duplicates;
        this.recordsRejected = rejected;
    }

    public void fail() {
        transitionTo(RunStatus.FAILED);
    }

    private void transitionTo(RunStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new IllegalStateException("Illegal run transition " + status + " -> " + next);
        }
        this.status = next;
        this.finishedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getSource() { return source; }
    public RunStatus getStatus() { return status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public int getRecordsRead() { return recordsRead; }
    public int getRecordsLoaded() { return recordsLoaded; }
    public int getDuplicatesSkipped() { return duplicatesSkipped; }
    public int getRecordsRejected() { return recordsRejected; }
}
