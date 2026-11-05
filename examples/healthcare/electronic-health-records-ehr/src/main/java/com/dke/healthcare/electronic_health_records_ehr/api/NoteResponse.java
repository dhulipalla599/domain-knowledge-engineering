package com.dke.healthcare.electronic_health_records_ehr.api;

import com.dke.healthcare.electronic_health_records_ehr.domain.ClinicalNote;
import com.dke.healthcare.electronic_health_records_ehr.domain.NoteStatus;
import java.util.List;

public record NoteResponse(Long id, String patientId, String authorId, String text, NoteStatus status, List<String> addenda) {

    static NoteResponse from(ClinicalNote n) {
        return new NoteResponse(n.getId(), n.getPatientId(), n.getAuthorId(), n.getText(), n.getStatus(), n.getAddenda());
    }
}
