package com.dke.data_engineering.change_data_capture_cdc_pipelines.domain;

import jakarta.persistence.*;

/** Dead-letter record: an invalid change event kept for inspection instead of being silently dropped. */
@Entity
@Table(name = "rejected_event")
public class RejectedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long runId;
    private long lsn;
    private String reason;
    @Column(length = 1000)
    private String payload;

    protected RejectedEvent() {
    }

    public RejectedEvent(Long runId, long lsn, String reason, String payload) {
        this.runId = runId;
        this.lsn = lsn;
        this.reason = reason;
        this.payload = payload;
    }

    public Long getId() { return id; }
    public Long getRunId() { return runId; }
    public long getLsn() { return lsn; }
    public String getReason() { return reason; }
    public String getPayload() { return payload; }
}
