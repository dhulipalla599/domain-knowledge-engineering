package com.dke.healthcare.electronic_health_records_ehr.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.time.Instant;

/** Append-only record of who touched which chart and why. */
@Entity
public class AuditEntry {

    @Id
    @GeneratedValue
    private Long id;
    private String actorId;
    private String patientId;
    private String action;
    private String reason;
    private Instant at;

    protected AuditEntry() {
    }

    public AuditEntry(String actorId, String patientId, String action, String reason, Instant at) {
        this.actorId = actorId;
        this.patientId = patientId;
        this.action = action;
        this.reason = reason;
        this.at = at;
    }

    public Long getId() { return id; }
    public String getActorId() { return actorId; }
    public String getPatientId() { return patientId; }
    public String getAction() { return action; }
    public String getReason() { return reason; }
    public Instant getAt() { return at; }
}
