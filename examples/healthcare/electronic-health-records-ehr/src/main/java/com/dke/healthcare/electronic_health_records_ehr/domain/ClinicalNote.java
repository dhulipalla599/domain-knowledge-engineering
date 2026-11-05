package com.dke.healthcare.electronic_health_records_ehr.domain;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** A clinical note. Once signed it is never edited in place: corrections are addenda. */
@Entity
public class ClinicalNote {

    @Id
    @GeneratedValue
    private Long id;
    private String patientId;
    private String authorId;
    @Column(length = 4000)
    private String text;
    @Enumerated(EnumType.STRING)
    private NoteStatus status = NoteStatus.DRAFT;
    private Instant signedAt;
    @ElementCollection(fetch = FetchType.EAGER)
    @Column(length = 4000)
    private List<String> addenda = new ArrayList<>();

    protected ClinicalNote() {
    }

    public ClinicalNote(String patientId, String authorId, String text) {
        this.patientId = patientId;
        this.authorId = authorId;
        this.text = text;
    }

    public void edit(String newText) {
        if (status != NoteStatus.DRAFT) {
            throw new IllegalStateException("Note " + id + " is " + status + " and cannot be edited; add an addendum instead");
        }
        this.text = newText;
    }

    public void sign(Instant now) {
        if (status != NoteStatus.DRAFT) {
            throw new IllegalStateException("Only a DRAFT note can be signed, but note " + id + " is " + status);
        }
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("An empty note cannot be signed");
        }
        this.status = NoteStatus.SIGNED;
        this.signedAt = now;
    }

    public void addAddendum(String addendumText) {
        if (status != NoteStatus.SIGNED && status != NoteStatus.AMENDED) {
            throw new IllegalStateException("Addenda apply to signed notes only; note " + id + " is " + status);
        }
        this.addenda.add(addendumText);
        this.status = NoteStatus.AMENDED;
    }

    public void markEnteredInError() {
        if (status == NoteStatus.DRAFT || status == NoteStatus.ENTERED_IN_ERROR) {
            throw new IllegalStateException("Only a signed or amended note can be entered in error, but note " + id + " is " + status);
        }
        this.status = NoteStatus.ENTERED_IN_ERROR;
    }

    public Long getId() { return id; }
    public String getPatientId() { return patientId; }
    public String getAuthorId() { return authorId; }
    public String getText() { return text; }
    public NoteStatus getStatus() { return status; }
    public Instant getSignedAt() { return signedAt; }
    public List<String> getAddenda() { return List.copyOf(addenda); }
}
