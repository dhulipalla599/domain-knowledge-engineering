package com.dke.insurance.policy_administration.repository;

import com.dke.insurance.policy_administration.domain.Policy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyRepository extends JpaRepository<Policy, Long> {
    boolean existsByQuoteId(String quoteId);
}
