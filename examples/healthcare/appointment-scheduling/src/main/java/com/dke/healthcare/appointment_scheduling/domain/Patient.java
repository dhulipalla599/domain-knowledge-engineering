package com.dke.healthcare.appointment_scheduling.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Patient {

    @Id
    private String id;
    private String name;
    private String insuranceMemberId;

    protected Patient() {
    }

    public Patient(String id, String name, String insuranceMemberId) {
        this.id = id;
        this.name = name;
        this.insuranceMemberId = insuranceMemberId;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getInsuranceMemberId() { return insuranceMemberId; }
}
