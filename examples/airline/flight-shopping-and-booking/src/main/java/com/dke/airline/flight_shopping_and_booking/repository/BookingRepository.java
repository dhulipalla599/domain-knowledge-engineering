package com.dke.airline.flight_shopping_and_booking.repository;

import com.dke.airline.flight_shopping_and_booking.domain.Booking;
import com.dke.airline.flight_shopping_and_booking.domain.BookingStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Optional<Booking> findByIdempotencyKey(String idempotencyKey);

    List<Booking> findByStatusAndHoldExpiresAtBefore(BookingStatus status, Instant cutoff);
}
