package com.dke.healthcare.electronic_health_records_ehr.repository;

import com.dke.healthcare.electronic_health_records_ehr.domain.AuditEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEntryRepository extends JpaRepository<AuditEntry, Long> {

    List<AuditEntry> findByPatientIdOrderByIdAsc(String patientId);
}
