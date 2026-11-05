package com.dke.healthcare.electronic_health_records_ehr.service;

import com.dke.healthcare.electronic_health_records_ehr.domain.AuditEntry;
import com.dke.healthcare.electronic_health_records_ehr.domain.ClinicalNote;
import com.dke.healthcare.electronic_health_records_ehr.domain.EhrEvents.ChartAccessed;
import com.dke.healthcare.electronic_health_records_ehr.domain.EhrEvents.NoteAmended;
import com.dke.healthcare.electronic_health_records_ehr.domain.EhrEvents.NoteSigned;
import com.dke.healthcare.electronic_health_records_ehr.domain.Patient;
import com.dke.healthcare.electronic_health_records_ehr.integration.CareTeamDirectory;
import com.dke.healthcare.electronic_health_records_ehr.repository.AuditEntryRepository;
import com.dke.healthcare.electronic_health_records_ehr.repository.ClinicalNoteRepository;
import com.dke.healthcare.electronic_health_records_ehr.repository.PatientRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EhrService {

    private final PatientRepository patients;
    private final ClinicalNoteRepository notes;
    private final AuditEntryRepository audit;
    private final CareTeamDirectory careTeams;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public EhrService(PatientRepository patients, ClinicalNoteRepository notes, AuditEntryRepository audit,
                      CareTeamDirectory careTeams, ApplicationEventPublisher events, Clock clock) {
        this.patients = patients;
        this.notes = notes;
        this.audit = audit;
        this.careTeams = careTeams;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public ClinicalNote createDraft(String patientId, String authorId, String text) {
        requirePatient(patientId);
        requireAccess(authorId, patientId, null);
        return notes.save(new ClinicalNote(patientId, authorId, text));
    }

    @Transactional
    public ClinicalNote editDraft(Long noteId, String authorId, String text) {
        ClinicalNote note = requireAuthoredNote(noteId, authorId);
        note.edit(text);
        return note;
    }

    @Transactional
    public ClinicalNote sign(Long noteId, String authorId) {
        ClinicalNote note = requireAuthoredNote(noteId, authorId);
        note.sign(clock.instant());
        events.publishEvent(new NoteSigned(note.getId(), note.getPatientId(), authorId));
        return note;
    }

    @Transactional
    public ClinicalNote addAddendum(Long noteId, String authorId, String text) {
        ClinicalNote note = requireAuthoredNote(noteId, authorId);
        note.addAddendum(text);
        events.publishEvent(new NoteAmended(note.getId(), note.getPatientId(), authorId));
        return note;
    }

    /** Key rule: a chart is readable only with a treatment relationship, or with a recorded break-glass reason. */
    @Transactional
    public List<ClinicalNote> readChart(String patientId, String clinicianId, String breakGlassReason) {
        requirePatient(patientId);
        boolean breakGlass = requireAccess(clinicianId, patientId, breakGlassReason);
        audit.save(new AuditEntry(clinicianId, patientId, breakGlass ? "BREAK_GLASS_READ" : "READ", breakGlassReason, clock.instant()));
        events.publishEvent(new ChartAccessed(patientId, clinicianId, breakGlass, breakGlassReason));
        return notes.findByPatientIdOrderByIdAsc(patientId);
    }

    @Transactional(readOnly = true)
    public List<AuditEntry> auditTrail(String patientId) {
        return audit.findByPatientIdOrderByIdAsc(patientId);
    }

    /** Returns true when access was granted through break-glass. */
    private boolean requireAccess(String clinicianId, String patientId, String breakGlassReason) {
        if (careTeams.hasTreatmentRelationship(clinicianId, patientId)) {
            return false;
        }
        if (breakGlassReason == null || breakGlassReason.isBlank()) {
            throw new ChartAccessDeniedException(
                    clinicianId + " has no treatment relationship with " + patientId + "; supply a break-glass reason for emergency access");
        }
        return true;
    }

    private ClinicalNote requireAuthoredNote(Long noteId, String authorId) {
        ClinicalNote note = notes.findById(noteId).orElseThrow(() -> new NotFoundException("Note " + noteId + " not found"));
        if (!note.getAuthorId().equals(authorId)) {
            throw new ChartAccessDeniedException("Only the author " + note.getAuthorId() + " may change note " + noteId);
        }
        return note;
    }

    private Patient requirePatient(String patientId) {
        return patients.findById(patientId).orElseThrow(() -> new NotFoundException("Patient " + patientId + " not found"));
    }
}
