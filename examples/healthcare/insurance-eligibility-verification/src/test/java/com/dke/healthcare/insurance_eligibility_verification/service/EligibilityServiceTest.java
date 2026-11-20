package com.dke.healthcare.insurance_eligibility_verification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dke.healthcare.insurance_eligibility_verification.domain.Coverage;
import com.dke.healthcare.insurance_eligibility_verification.domain.EligibilityCheck;
import com.dke.healthcare.insurance_eligibility_verification.domain.EligibilityStatus;
import com.dke.healthcare.insurance_eligibility_verification.domain.Payer;
import com.dke.healthcare.insurance_eligibility_verification.integration.PayerGateway;
import com.dke.healthcare.insurance_eligibility_verification.integration.PayerGateway.PayerResponse;
import com.dke.healthcare.insurance_eligibility_verification.repository.CoverageRepository;
import com.dke.healthcare.insurance_eligibility_verification.repository.EligibilityCheckRepository;
import com.dke.healthcare.insurance_eligibility_verification.repository.PayerRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class EligibilityServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 20);

    private final CoverageRepository coverages = mock(CoverageRepository.class);
    private final PayerRepository payers = mock(PayerRepository.class);
    private final EligibilityCheckRepository checks = mock(EligibilityCheckRepository.class);
    private final PayerGateway gateway = mock(PayerGateway.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private EligibilityService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-20T15:00:00Z"), ZoneOffset.UTC);
        service = new EligibilityService(coverages, payers, checks, gateway, events, clock);
        when(coverages.findById("cov-1")).thenReturn(Optional.of(
                new Coverage("cov-1", "pat-1", "NORTHWIND", "NWH1", "Maria", "Alvarez", LocalDate.of(1988, 4, 12))));
        when(payers.findById("NORTHWIND")).thenReturn(Optional.of(new Payer("NORTHWIND", "Northwind Health Plan", true)));
        when(checks.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void rejectsServiceDateBeyondTheInquiryWindow() {
        assertThatThrownBy(() -> service.verify("cov-1", TODAY.plusDays(31), "30"))
                .isInstanceOf(EligibilityRuleException.class)
                .hasMessageContaining("outside the inquiry window");
        verifyNoInteractions(gateway);
    }

    @Test
    void rejectsPayerWithoutRealTimeEligibility() {
        when(payers.findById("NORTHWIND")).thenReturn(Optional.of(new Payer("NORTHWIND", "Northwind Health Plan", false)));
        assertThatThrownBy(() -> service.verify("cov-1", TODAY, "30"))
                .isInstanceOf(EligibilityRuleException.class)
                .hasMessageContaining("does not support real-time eligibility");
    }

    @Test
    void activeFlagOutsidePlanDatesIsInactive() {
        when(gateway.inquire(any())).thenReturn(new PayerResponse(true, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 10, 31),
                new BigDecimal("30.00"), BigDecimal.ZERO, null, null));
        EligibilityCheck check = service.verify("cov-1", TODAY, "30").check();
        assertThat(check.getStatus()).isEqualTo(EligibilityStatus.INACTIVE);
    }

    @Test
    void onlyPayerErrorsCanBeRetried() {
        EligibilityCheck check = new EligibilityCheck("chk-1", "cov-1", TODAY, "30", Instant.now());
        check.markActive(TODAY, TODAY, BigDecimal.ONE, BigDecimal.ONE, Instant.now());
        assertThatThrownBy(() -> check.resubmit(Instant.now())).isInstanceOf(IllegalStateException.class);
        assertThat(EligibilityStatus.PAYER_ERROR.canMoveTo(EligibilityStatus.PENDING)).isTrue();
    }
}
