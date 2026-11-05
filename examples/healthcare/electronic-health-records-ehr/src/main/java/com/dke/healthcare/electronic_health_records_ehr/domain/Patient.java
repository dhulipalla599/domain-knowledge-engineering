package com.dke.healthcare.electronic_health_records_ehr.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Patient {

    @Id
    private String id;
    private String name;
    private String mrn;

    protected Patient() {
    }

    public Patient(String id, String name, String mrn) {
        this.id = id;
        this.name = name;
        this.mrn = mrn;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getMrn() { return mrn; }
}
