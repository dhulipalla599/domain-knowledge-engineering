package com.dke.healthcare.electronic_health_records_ehr.repository;

import com.dke.healthcare.electronic_health_records_ehr.domain.ClinicalNote;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicalNoteRepository extends JpaRepository<ClinicalNote, Long> {

    List<ClinicalNote> findByPatientIdOrderByIdAsc(String patientId);
}
