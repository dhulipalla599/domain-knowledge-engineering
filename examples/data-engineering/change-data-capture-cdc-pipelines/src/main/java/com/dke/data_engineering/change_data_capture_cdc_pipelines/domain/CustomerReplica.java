package com.dke.data_engineering.change_data_capture_cdc_pipelines.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** The analytics copy of a source customer row. Deletes are soft so history is kept. */
@Entity
@Table(name = "customer_replica")
public class CustomerReplica {

    @Id
    private Long customerId;
    private String email;
    private String fullName;
    private String city;
    private boolean deleted;

    /** Log sequence number of the last change applied; the guard against stale and duplicate events. */
    @Column(nullable = false)
    private long lastLsn;

    private Instant updatedAt;

    protected CustomerReplica() {
    }

    public CustomerReplica(Long customerId) {
        this.customerId = customerId;
    }

    public void apply(ChangeEvent event) {
        if ("d".equals(event.op())) {
            deleted = true;
        } else {
            email = event.email();
            fullName = event.fullName();
            city = event.city();
            deleted = false;
        }
        lastLsn = event.lsn();
        updatedAt = Instant.now();
    }

    public Long getCustomerId() { return customerId; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getCity() { return city; }
    public boolean isDeleted() { return deleted; }
    public long getLastLsn() { return lastLsn; }
    public Instant getUpdatedAt() { return updatedAt; }
}
