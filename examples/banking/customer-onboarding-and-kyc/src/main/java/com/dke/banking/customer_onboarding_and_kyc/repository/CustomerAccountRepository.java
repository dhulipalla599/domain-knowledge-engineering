package com.dke.banking.customer_onboarding_and_kyc.repository;

import com.dke.banking.customer_onboarding_and_kyc.domain.CustomerAccount;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerAccountRepository extends JpaRepository<CustomerAccount, UUID> {

    Optional<CustomerAccount> findByApplicationId(UUID applicationId);
}
