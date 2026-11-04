package com.dke.healthcare.appointment_scheduling.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Provider {

    @Id
    private String id;
    private String name;
    private String specialty;
    private int slotMinutes;

    protected Provider() {
    }

    public Provider(String id, String name, String specialty, int slotMinutes) {
        this.id = id;
        this.name = name;
        this.specialty = specialty;
        this.slotMinutes = slotMinutes;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getSpecialty() { return specialty; }
    public int getSlotMinutes() { return slotMinutes; }
}
