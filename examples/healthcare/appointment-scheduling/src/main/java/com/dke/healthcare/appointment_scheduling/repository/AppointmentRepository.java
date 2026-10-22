package com.dke.healthcare.appointment_scheduling.repository;

import com.dke.healthcare.appointment_scheduling.domain.Appointment;
import com.dke.healthcare.appointment_scheduling.domain.AppointmentStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    Optional<Appointment> findByIdempotencyKey(String idempotencyKey);

    /** True when an active appointment for the provider overlaps [start, end). */
    boolean existsByProviderIdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
            String providerId, Collection<AppointmentStatus> statuses, Instant end, Instant start);

    List<Appointment> findByProviderIdOrderByStartTime(String providerId);
}
