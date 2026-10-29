package com.dke.retail.inventory_management.repository;

import com.dke.retail.inventory_management.domain.Reservation;
import com.dke.retail.inventory_management.domain.ReservationStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, String> {
    Optional<Reservation> findByOrderRefAndSku(String orderRef, String sku);

    List<Reservation> findByStatusAndExpiresAtBefore(ReservationStatus status, Instant cutoff);
}
