package com.dke.healthcare.insurance_eligibility_verification.repository;

import com.dke.healthcare.insurance_eligibility_verification.domain.Coverage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoverageRepository extends JpaRepository<Coverage, String> {
}
