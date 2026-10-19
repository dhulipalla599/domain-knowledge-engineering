package com.dke.telecom.subscriber_onboarding.repository;

import com.dke.telecom.subscriber_onboarding.domain.PhoneNumber;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhoneNumberRepository extends JpaRepository<PhoneNumber, String> {

    Optional<PhoneNumber> findFirstByStatusOrderByMsisdn(PhoneNumber.Status status);
}
