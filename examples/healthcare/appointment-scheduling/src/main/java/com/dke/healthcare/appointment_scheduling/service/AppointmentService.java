package com.dke.healthcare.appointment_scheduling.service;

import com.dke.healthcare.appointment_scheduling.domain.Appointment;
import com.dke.healthcare.appointment_scheduling.domain.AppointmentEvents.AppointmentBooked;
import com.dke.healthcare.appointment_scheduling.domain.AppointmentEvents.AppointmentCancelled;
import com.dke.healthcare.appointment_scheduling.domain.AppointmentEvents.PatientCheckedIn;
import com.dke.healthcare.appointment_scheduling.domain.AppointmentStatus;
import com.dke.healthcare.appointment_scheduling.domain.Patient;
import com.dke.healthcare.appointment_scheduling.domain.Provider;
import com.dke.healthcare.appointment_scheduling.integration.EligibilityChecker;
import com.dke.healthcare.appointment_scheduling.repository.AppointmentRepository;
import com.dke.healthcare.appointment_scheduling.repository.PatientRepository;
import com.dke.healthcare.appointment_scheduling.repository.ProviderRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentService {

    static final Duration MIN_LEAD_TIME = Duration.ofHours(1);
    static final Duration LATE_CANCELLATION_WINDOW = Duration.ofHours(24);

    public record BookingOutcome(Appointment appointment, boolean created) {
    }

    private final AppointmentRepository appointments;
    private final ProviderRepository providers;
    private final PatientRepository patients;
    private final EligibilityChecker eligibility;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public AppointmentService(AppointmentRepository appointments, ProviderRepository providers,
                              PatientRepository patients, EligibilityChecker eligibility,
                              ApplicationEventPublisher events, Clock clock) {
        this.appointments = appointments;
        this.providers = providers;
        this.patients = patients;
        this.eligibility = eligibility;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public BookingOutcome book(String patientId, String providerId, Instant start, String reason, String idempotencyKey) {
        if (idempotencyKey != null) {
            var existing = appointments.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return new BookingOutcome(existing.get(), false);
            }
        }
        Patient patient = patients.findById(patientId)
                .orElseThrow(() -> new NotFoundException("Patient " + patientId + " not found"));
        Provider provider = providers.findById(providerId)
                .orElseThrow(() -> new NotFoundException("Provider " + providerId + " not found"));

        if (start.isBefore(clock.instant().plus(MIN_LEAD_TIME))) {
            throw new BookingRuleException("Appointments must be booked at least 1 hour ahead");
        }
        if (start.getEpochSecond() % (provider.getSlotMinutes() * 60L) != 0) {
            throw new BookingRuleException("Start time must align to the provider's "
                    + provider.getSlotMinutes() + "-minute slots");
        }
        if (!eligibility.isEligible(patient.getInsuranceMemberId())) {
            throw new BookingRuleException("Insurance coverage could not be verified for patient " + patientId);
        }
        Instant end = start.plus(Duration.ofMinutes(provider.getSlotMinutes()));
        if (appointments.existsByProviderIdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                providerId, AppointmentStatus.HOLDING_SLOT, end, start)) {
            throw new SlotConflictException("Provider " + providerId + " is already booked at " + start);
        }

        Appointment saved = appointments.save(new Appointment(patientId, providerId, start, end, reason, idempotencyKey));
        events.publishEvent(new AppointmentBooked(saved.getId(), patientId, providerId, start));
        return new BookingOutcome(saved, true);
    }

    @Transactional
    public Appointment cancel(UUID id) {
        Appointment appointment = get(id);
        appointment.transitionTo(AppointmentStatus.CANCELLED);
        boolean late = appointment.getStartTime().isBefore(clock.instant().plus(LATE_CANCELLATION_WINDOW));
        events.publishEvent(new AppointmentCancelled(id, appointment.getProviderId(), appointment.getStartTime(), late));
        return appointment;
    }

    @Transactional
    public Appointment checkIn(UUID id) {
        Appointment appointment = get(id);
        appointment.transitionTo(AppointmentStatus.CHECKED_IN);
        events.publishEvent(new PatientCheckedIn(id, appointment.getProviderId()));
        return appointment;
    }

    @Transactional(readOnly = true)
    public Appointment get(UUID id) {
        return appointments.findById(id).orElseThrow(() -> new NotFoundException("Appointment " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<Appointment> forProvider(String providerId) {
        return appointments.findByProviderIdOrderByStartTime(providerId);
    }
}
