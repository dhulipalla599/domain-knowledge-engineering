package com.dke.healthcare.patient_registration_and_master_patient_index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dke.healthcare.patient_registration_and_master_patient_index.domain.IllegalStateTransitionException;
import com.dke.healthcare.patient_registration_and_master_patient_index.domain.Patient;
import com.dke.healthcare.patient_registration_and_master_patient_index.domain.PatientStatus;
import com.dke.healthcare.patient_registration_and_master_patient_index.service.MatchScorer;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class MatchScorerTest {

    private final LocalDate dob = LocalDate.of(1988, 3, 14);
    private final Patient existing =
            new Patient("Amara", "Okafor", dob, "555-010-1234", "M13 9PL", PatientStatus.ACTIVE);

    @Test
    void sameNameDobAndPhoneIsADuplicate() {
        int score = MatchScorer.score("amara", "OKAFOR", dob, "(555) 010 1234", null, existing);
        assertThat(score).isGreaterThanOrEqualTo(MatchScorer.DUPLICATE_THRESHOLD);
    }

    @Test
    void typoInSurnameWithoutPhoneNeedsReview() {
        int score = MatchScorer.score("Amara", "Okafur", dob, null, null, existing);
        assertThat(score).isBetween(MatchScorer.REVIEW_THRESHOLD, MatchScorer.DUPLICATE_THRESHOLD - 1);
    }

    @Test
    void differentDateOfBirthNeverMatches() {
        assertThat(MatchScorer.score("Amara", "Okafor", dob.plusDays(1), "5550101234", "M139PL", existing)).isZero();
    }

    @Test
    void mergedRecordIsTerminal() {
        Patient survivor = new Patient("Amara", "Okafor", dob, null, null, PatientStatus.ACTIVE);
        existing.mergeInto(survivor);
        assertThatThrownBy(existing::approveAsDistinct).isInstanceOf(IllegalStateTransitionException.class);
    }
}
