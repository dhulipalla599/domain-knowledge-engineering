package com.dke.telecom.subscriber_onboarding.repository;

import com.dke.telecom.subscriber_onboarding.domain.SimCard;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SimCardRepository extends JpaRepository<SimCard, String> {
}
