package com.dke.healthcare.patient_registration_and_master_patient_index.repository;

import com.dke.healthcare.patient_registration_and_master_patient_index.domain.Patient;
import com.dke.healthcare.patient_registration_and_master_patient_index.domain.PatientStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByMrn(String mrn);

    /** Blocking step: only people with the same date of birth are scored, never the whole index. */
    List<Patient> findByDateOfBirthAndStatusNot(LocalDate dateOfBirth, PatientStatus excluded);

    List<Patient> findByStatus(PatientStatus status);
}
