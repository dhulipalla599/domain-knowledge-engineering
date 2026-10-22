package com.dke.healthcare.appointment_scheduling.repository;

import com.dke.healthcare.appointment_scheduling.domain.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, String> {
}
