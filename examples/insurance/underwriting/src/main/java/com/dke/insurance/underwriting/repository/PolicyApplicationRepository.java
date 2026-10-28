package com.dke.insurance.underwriting.repository;

import com.dke.insurance.underwriting.domain.ApplicationStatus;
import com.dke.insurance.underwriting.domain.PolicyApplication;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyApplicationRepository extends JpaRepository<PolicyApplication, Long> {

    Optional<PolicyApplication> findByIdempotencyKey(String idempotencyKey);

    List<PolicyApplication> findByStatusOrderByCreatedAtAsc(ApplicationStatus status);
}
