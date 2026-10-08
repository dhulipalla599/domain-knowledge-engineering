package com.dke.healthcare.patient_registration_and_master_patient_index.service;

import com.dke.healthcare.patient_registration_and_master_patient_index.domain.DuplicatePatientException;
import com.dke.healthcare.patient_registration_and_master_patient_index.domain.InvalidRegistrationException;
import com.dke.healthcare.patient_registration_and_master_patient_index.domain.MatchReviewRequested;
import com.dke.healthcare.patient_registration_and_master_patient_index.domain.Patient;
import com.dke.healthcare.patient_registration_and_master_patient_index.domain.PatientNotFoundException;
import com.dke.healthcare.patient_registration_and_master_patient_index.domain.PatientRegistered;
import com.dke.healthcare.patient_registration_and_master_patient_index.domain.PatientStatus;
import com.dke.healthcare.patient_registration_and_master_patient_index.domain.PatientsMerged;
import com.dke.healthcare.patient_registration_and_master_patient_index.repository.PatientRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private final PatientRepository patients;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public RegistrationService(PatientRepository patients, ApplicationEventPublisher events, Clock clock) {
        this.patients = patients;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Core rule: score the new registration against everyone with the same date of birth.
     * Strong match -> reject; possible match -> register but hold for a human; otherwise ACTIVE.
     */
    @Transactional
    public Patient register(String firstName, String lastName, LocalDate dob, String phone, String postcode) {
        if (dob.isAfter(LocalDate.now(clock))) {
            throw new InvalidRegistrationException("Date of birth cannot be in the future");
        }
        Patient best = null;
        int bestScore = 0;
        for (Patient existing : patients.findByDateOfBirthAndStatusNot(dob, PatientStatus.MERGED)) {
            int score = MatchScorer.score(firstName, lastName, dob, phone, postcode, existing);
            if (score > bestScore) {
                best = existing;
                bestScore = score;
            }
        }
        if (bestScore >= MatchScorer.DUPLICATE_THRESHOLD) {
            throw new DuplicatePatientException(best.getMrn(), bestScore);
        }
        boolean needsReview = bestScore >= MatchScorer.REVIEW_THRESHOLD;
        Patient saved = patients.save(new Patient(firstName, lastName, dob, phone, postcode,
                needsReview ? PatientStatus.PENDING_REVIEW : PatientStatus.ACTIVE));
        events.publishEvent(needsReview
                ? new MatchReviewRequested(saved.getMrn(), best.getMrn(), bestScore)
                : new PatientRegistered(saved.getMrn()));
        return saved;
    }

    @Transactional
    public Patient approveAsDistinct(String mrn) {
        Patient patient = find(mrn);
        patient.approveAsDistinct();
        events.publishEvent(new PatientRegistered(mrn));
        return patient;
    }

    @Transactional
    public Patient merge(String mrn, String survivorMrn) {
        Patient patient = find(mrn);
        patient.mergeInto(find(survivorMrn));
        events.publishEvent(new PatientsMerged(mrn, survivorMrn));
        return patient;
    }

    @Transactional(readOnly = true)
    public Patient find(String mrn) {
        return patients.findByMrn(mrn).orElseThrow(() -> new PatientNotFoundException(mrn));
    }

    @Transactional(readOnly = true)
    public List<Patient> reviewQueue() {
        return patients.findByStatus(PatientStatus.PENDING_REVIEW);
    }
}
