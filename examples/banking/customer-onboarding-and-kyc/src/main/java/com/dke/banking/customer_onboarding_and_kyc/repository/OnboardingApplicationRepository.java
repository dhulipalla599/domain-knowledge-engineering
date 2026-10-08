package com.dke.banking.customer_onboarding_and_kyc.repository;

import com.dke.banking.customer_onboarding_and_kyc.domain.ApplicationStatus;
import com.dke.banking.customer_onboarding_and_kyc.domain.OnboardingApplication;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OnboardingApplicationRepository extends JpaRepository<OnboardingApplication, UUID> {

    Optional<OnboardingApplication> findByIdempotencyKey(String idempotencyKey);

    List<OnboardingApplication> findByStatusOrderByCreatedAtAsc(ApplicationStatus status);
}
