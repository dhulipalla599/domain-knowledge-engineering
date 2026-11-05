package com.dke.healthcare.electronic_health_records_ehr.integration;

import com.dke.healthcare.electronic_health_records_ehr.domain.EhrEvents.ChartAccessed;
import com.dke.healthcare.electronic_health_records_ehr.domain.EhrEvents.NoteAmended;
import com.dke.healthcare.electronic_health_records_ehr.domain.EhrEvents.NoteSigned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Stands in for Kafka consumers: a FHIR export job, a patient-portal notifier and a privacy-office alert. */
@Component
public class EhrEventListener {

    private static final Logger log = LoggerFactory.getLogger(EhrEventListener.class);

    @EventListener
    void onNoteSigned(NoteSigned e) {
        log.info("ehr.note.signed -> publish DocumentReference for note {} (patient {})", e.noteId(), e.patientId());
    }

    @EventListener
    void onNoteAmended(NoteAmended e) {
        log.info("ehr.note.amended -> notify downstream consumers of note {}", e.noteId());
    }

    @EventListener
    void onChartAccessed(ChartAccessed e) {
        if (e.breakGlass()) {
            log.warn("ehr.chart.accessed -> BREAK-GLASS by {} on {}: {}", e.actorId(), e.patientId(), e.reason());
        }
    }
}
