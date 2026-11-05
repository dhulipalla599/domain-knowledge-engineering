package com.dke.healthcare.electronic_health_records_ehr.domain;

/** In-process stand-ins for Kafka topics: ehr.note.signed, ehr.note.amended, ehr.chart.accessed. */
public final class EhrEvents {

    private EhrEvents() {
    }

    public record NoteSigned(Long noteId, String patientId, String authorId) { }

    public record NoteAmended(Long noteId, String patientId, String authorId) { }

    public record ChartAccessed(String patientId, String actorId, boolean breakGlass, String reason) { }
}
