package com.dke.healthcare.insurance_eligibility_verification.repository;

import com.dke.healthcare.insurance_eligibility_verification.domain.EligibilityCheck;
import com.dke.healthcare.insurance_eligibility_verification.domain.EligibilityStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EligibilityCheckRepository extends JpaRepository<EligibilityCheck, String> {

    /** Most recent completed answer for the same question, if it is still fresh. */
    Optional<EligibilityCheck> findFirstByCoverageIdAndServiceDateAndServiceTypeCodeAndStatusInAndCompletedAtAfterOrderByCompletedAtDesc(
            String coverageId, LocalDate serviceDate, String serviceTypeCode,
            Collection<EligibilityStatus> statuses, Instant completedAfter);

    List<EligibilityCheck> findByCoverageIdOrderByRequestedAtDesc(String coverageId);
}
