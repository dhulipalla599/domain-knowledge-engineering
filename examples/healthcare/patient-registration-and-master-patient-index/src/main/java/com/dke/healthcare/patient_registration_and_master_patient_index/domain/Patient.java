package com.dke.healthcare.patient_registration_and_master_patient_index.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDate;
import java.util.UUID;

/** One record in the master patient index. Identified by a Medical Record Number (MRN). */
@Entity
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String mrn;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    private String phone;
    private String postcode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PatientStatus status;

    /** Set only when status is MERGED: the MRN that now represents this person. */
    private String survivorMrn;

    protected Patient() {
    }

    public Patient(String firstName, String lastName, LocalDate dateOfBirth, String phone, String postcode,
                   PatientStatus initialStatus) {
        this.mrn = "MRN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.firstName = firstName.trim();
        this.lastName = lastName.trim();
        this.dateOfBirth = dateOfBirth;
        this.phone = phone == null ? null : phone.replaceAll("\\D", "");
        this.postcode = postcode == null ? null : postcode.replaceAll("\\s", "").toUpperCase();
        this.status = initialStatus;
    }

    /** Enforces the legal lifecycle transitions; anything else is a business error. */
    private void moveTo(PatientStatus next) {
        if (!status.allowedNext().contains(next)) {
            throw new IllegalStateTransitionException(mrn, status, next);
        }
        status = next;
    }

    public void approveAsDistinct() {
        moveTo(PatientStatus.ACTIVE);
    }

    public void mergeInto(Patient survivor) {
        if (survivor == this || survivor.mrn.equals(mrn)) {
            throw new IllegalStateTransitionException(mrn, status, PatientStatus.MERGED);
        }
        if (survivor.status != PatientStatus.ACTIVE) {
            throw new IllegalStateTransitionException(survivor.mrn, survivor.status, PatientStatus.ACTIVE);
        }
        moveTo(PatientStatus.MERGED);
        this.survivorMrn = survivor.mrn;
    }

    public Long getId() { return id; }
    public String getMrn() { return mrn; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public String getPhone() { return phone; }
    public String getPostcode() { return postcode; }
    public PatientStatus getStatus() { return status; }
    public String getSurvivorMrn() { return survivorMrn; }
}
