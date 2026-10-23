package com.dke.healthcare.appointment_scheduling.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
public class Appointment {

    @Id
    private UUID id = UUID.randomUUID();
    private String patientId;
    private String providerId;
    private Instant startTime;
    private Instant endTime;
    private String reason;
    @Column(unique = true)
    private String idempotencyKey;
    @Enumerated(EnumType.STRING)
    private AppointmentStatus status = AppointmentStatus.BOOKED;
    @Version
    private long version;

    protected Appointment() {
    }

    public Appointment(String patientId, String providerId, Instant startTime, Instant endTime,
                       String reason, String idempotencyKey) {
        this.patientId = patientId;
        this.providerId = providerId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.reason = reason;
        this.idempotencyKey = idempotencyKey;
    }

    /** Moves to the next status, rejecting any transition the lifecycle does not allow. */
    public void transitionTo(AppointmentStatus next) {
        if (!status.allowedNext().contains(next)) {
            throw new IllegalStateException("Cannot move appointment from " + status + " to " + next);
        }
        this.status = next;
    }

    public UUID getId() { return id; }
    public String getPatientId() { return patientId; }
    public String getProviderId() { return providerId; }
    public Instant getStartTime() { return startTime; }
    public Instant getEndTime() { return endTime; }
    public String getReason() { return reason; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public AppointmentStatus getStatus() { return status; }
}
