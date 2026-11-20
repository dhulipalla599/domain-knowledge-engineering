package com.dke.banking.card_authorization_and_settlement.repository;

import com.dke.banking.card_authorization_and_settlement.domain.Authorization;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorizationRepository extends JpaRepository<Authorization, UUID> {

    Optional<Authorization> findByNetworkReference(String networkReference);

    List<Authorization> findByCardTokenOrderByCreatedAtDesc(String cardToken);
}
