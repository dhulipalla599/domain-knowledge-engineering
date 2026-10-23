package com.dke.healthcare.appointment_scheduling.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dke.healthcare.appointment_scheduling.domain.Patient;
import com.dke.healthcare.appointment_scheduling.domain.Provider;
import com.dke.healthcare.appointment_scheduling.integration.EligibilityChecker;
import com.dke.healthcare.appointment_scheduling.repository.AppointmentRepository;
import com.dke.healthcare.appointment_scheduling.repository.PatientRepository;
import com.dke.healthcare.appointment_scheduling.repository.ProviderRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class AppointmentServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-02T08:00:00Z");

    private final AppointmentRepository appointments = mock(AppointmentRepository.class);
    private final EligibilityChecker eligibility = mock(EligibilityChecker.class);
    private AppointmentService service;

    @BeforeEach
    void setUp() {
        var providers = mock(ProviderRepository.class);
        var patients = mock(PatientRepository.class);
        var provider = new Provider("p1", "Dr. Test", "Family medicine", 30);
        var patient = new Patient("a1", "Test Patient", "MBR-1");
        when(providers.findById("p1")).thenReturn(Optional.of(provider));
        when(patients.findById("a1")).thenReturn(Optional.of(patient));
        when(eligibility.isEligible("MBR-1")).thenReturn(true);
        when(appointments.save(any())).thenAnswer(i -> i.getArgument(0));
        service = new AppointmentService(appointments, providers, patients, eligibility,
                mock(ApplicationEventPublisher.class), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void booksAFreeSlot() {
        var outcome = service.book("a1", "p1", NOW.plusSeconds(3 * 3600), "checkup", null);
        assertThat(outcome.created()).isTrue();
        assertThat(outcome.appointment().getEndTime()).isEqualTo(NOW.plusSeconds(3 * 3600 + 1800));
    }

    @Test
    void rejectsOverlappingBooking() {
        when(appointments.existsByProviderIdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                any(), any(), any(), any())).thenReturn(true);
        assertThatThrownBy(() -> service.book("a1", "p1", NOW.plusSeconds(3 * 3600), "checkup", null))
                .isInstanceOf(SlotConflictException.class);
    }

    @Test
    void rejectsShortNoticeAndMisalignedStart() {
        assertThatThrownBy(() -> service.book("a1", "p1", NOW.plusSeconds(1800), null, null))
                .isInstanceOf(BookingRuleException.class).hasMessageContaining("1 hour");
        assertThatThrownBy(() -> service.book("a1", "p1", NOW.plusSeconds(3 * 3600 + 60), null, null))
                .isInstanceOf(BookingRuleException.class).hasMessageContaining("align");
    }

    @Test
    void rejectsLapsedCoverage() {
        when(eligibility.isEligible("MBR-1")).thenReturn(false);
        assertThatThrownBy(() -> service.book("a1", "p1", NOW.plusSeconds(3 * 3600), null, null))
                .isInstanceOf(BookingRuleException.class).hasMessageContaining("Insurance");
    }
}
