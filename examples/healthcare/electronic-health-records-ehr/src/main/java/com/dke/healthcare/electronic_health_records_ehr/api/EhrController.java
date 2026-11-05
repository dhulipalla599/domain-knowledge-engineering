package com.dke.healthcare.electronic_health_records_ehr.api;

import com.dke.healthcare.electronic_health_records_ehr.domain.AuditEntry;
import com.dke.healthcare.electronic_health_records_ehr.service.EhrService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EhrController {

    private final EhrService service;

    public EhrController(EhrService service) {
        this.service = service;
    }

    @PostMapping("/api/patients/{patientId}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    NoteResponse create(@PathVariable String patientId, @Valid @RequestBody NoteRequest req) {
        return NoteResponse.from(service.createDraft(patientId, req.authorId(), req.text()));
    }

    @PutMapping("/api/notes/{noteId}")
    NoteResponse edit(@PathVariable Long noteId, @Valid @RequestBody NoteRequest req) {
        return NoteResponse.from(service.editDraft(noteId, req.authorId(), req.text()));
    }

    @PostMapping("/api/notes/{noteId}/sign")
    NoteResponse sign(@PathVariable Long noteId, @Valid @RequestBody NoteRequest req) {
        return NoteResponse.from(service.sign(noteId, req.authorId()));
    }

    @PostMapping("/api/notes/{noteId}/addenda")
    NoteResponse addendum(@PathVariable Long noteId, @Valid @RequestBody NoteRequest req) {
        return NoteResponse.from(service.addAddendum(noteId, req.authorId(), req.text()));
    }

    @GetMapping("/api/patients/{patientId}/chart")
    List<NoteResponse> chart(@PathVariable String patientId, @RequestParam String clinicianId,
                             @RequestParam(required = false) String breakGlassReason) {
        return service.readChart(patientId, clinicianId, breakGlassReason).stream().map(NoteResponse::from).toList();
    }

    @GetMapping("/api/patients/{patientId}/audit")
    List<AuditEntry> audit(@PathVariable String patientId) {
        return service.auditTrail(patientId);
    }
}
