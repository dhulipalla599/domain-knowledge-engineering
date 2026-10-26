package com.dke.data_engineering.real_time_streaming_etl_with_kafka.domain;

import jakarta.persistence.*;

/** Dead-letter row: a raw line that failed validation, kept with the reason for replay. */
@Entity
@Table(name = "rejected_record")
public class RejectedRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long runId;
    @Column(length = 1000)
    private String rawLine;
    private String reason;

    protected RejectedRecord() {
    }

    public RejectedRecord(Long runId, String rawLine, String reason) {
        this.runId = runId;
        this.rawLine = rawLine;
        this.reason = reason;
    }

    public Long getId() { return id; }
    public Long getRunId() { return runId; }
    public String getRawLine() { return rawLine; }
    public String getReason() { return reason; }
}
