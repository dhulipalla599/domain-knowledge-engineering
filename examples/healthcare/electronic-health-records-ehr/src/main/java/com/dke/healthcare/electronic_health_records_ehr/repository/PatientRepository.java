package com.dke.healthcare.electronic_health_records_ehr.repository;

import com.dke.healthcare.electronic_health_records_ehr.domain.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, String> {
}
