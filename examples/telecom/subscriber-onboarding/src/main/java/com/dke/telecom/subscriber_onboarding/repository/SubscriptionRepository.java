package com.dke.telecom.subscriber_onboarding.repository;

import com.dke.telecom.subscriber_onboarding.domain.Subscription;
import com.dke.telecom.subscriber_onboarding.domain.SubscriptionStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByIdempotencyKey(String idempotencyKey);

    long countByIdDocumentNumberAndStatus(String idDocumentNumber, SubscriptionStatus status);
}
