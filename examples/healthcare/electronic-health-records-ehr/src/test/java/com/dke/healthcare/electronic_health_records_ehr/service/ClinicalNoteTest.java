package com.dke.healthcare.electronic_health_records_ehr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dke.healthcare.electronic_health_records_ehr.domain.ClinicalNote;
import com.dke.healthcare.electronic_health_records_ehr.domain.NoteStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ClinicalNoteTest {

    private final Instant now = Instant.parse("2026-11-05T10:00:00Z");

    @Test
    void signedNoteCannotBeEditedButCanBeAmended() {
        ClinicalNote note = new ClinicalNote("pat-amara", "dr-lee", "BP 150/95, start review");
        note.sign(now);

        assertThat(note.getStatus()).isEqualTo(NoteStatus.SIGNED);
        assertThatThrownBy(() -> note.edit("changed")).isInstanceOf(IllegalStateException.class);

        note.addAddendum("Correction: BP was 140/90");
        assertThat(note.getStatus()).isEqualTo(NoteStatus.AMENDED);
        assertThat(note.getText()).isEqualTo("BP 150/95, start review");
        assertThat(note.getAddenda()).containsExactly("Correction: BP was 140/90");
    }

    @Test
    void emptyNoteCannotBeSigned() {
        ClinicalNote note = new ClinicalNote("pat-amara", "dr-lee", " ");
        assertThatThrownBy(() -> note.sign(now)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void draftCannotBeAmended() {
        ClinicalNote note = new ClinicalNote("pat-amara", "dr-lee", "text");
        assertThatThrownBy(() -> note.addAddendum("x")).isInstanceOf(IllegalStateException.class);
    }
}
