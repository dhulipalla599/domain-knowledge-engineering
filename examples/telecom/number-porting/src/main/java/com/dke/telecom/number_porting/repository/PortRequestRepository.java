package com.dke.telecom.number_porting.repository;

import com.dke.telecom.number_porting.domain.PortRequest;
import com.dke.telecom.number_porting.domain.PortStatus;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortRequestRepository extends JpaRepository<PortRequest, Long> {

    Optional<PortRequest> findByIdempotencyKey(String idempotencyKey);

    boolean existsByMsisdnAndStatusIn(String msisdn, Collection<PortStatus> statuses);
}
