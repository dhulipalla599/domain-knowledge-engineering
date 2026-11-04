package com.dke.capital_markets.brokerage_account_opening.repository;

import com.dke.capital_markets.brokerage_account_opening.domain.AccountApplication;
import com.dke.capital_markets.brokerage_account_opening.domain.ApplicationStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountApplicationRepository extends JpaRepository<AccountApplication, UUID> {

    Optional<AccountApplication> findByIdempotencyKey(String idempotencyKey);

    List<AccountApplication> findByStatusOrderByCreatedAtDesc(ApplicationStatus status);
}
