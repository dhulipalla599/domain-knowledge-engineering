package com.dke.healthcare.insurance_eligibility_verification.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** A health plan the provider bills, and whether it answers real-time (270/271) inquiries. */
@Entity
public class Payer {

    @Id
    private String id;
    private String name;
    private boolean realTimeEnabled;

    protected Payer() {
    }

    public Payer(String id, String name, boolean realTimeEnabled) {
        this.id = id;
        this.name = name;
        this.realTimeEnabled = realTimeEnabled;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public boolean isRealTimeEnabled() { return realTimeEnabled; }
}
