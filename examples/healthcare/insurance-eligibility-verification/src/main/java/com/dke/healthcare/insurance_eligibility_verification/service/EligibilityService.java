package com.dke.healthcare.insurance_eligibility_verification.service;

import com.dke.healthcare.insurance_eligibility_verification.domain.Coverage;
import com.dke.healthcare.insurance_eligibility_verification.domain.EligibilityCheck;
import com.dke.healthcare.insurance_eligibility_verification.domain.EligibilityEvents.CoverageInactive;
import com.dke.healthcare.insurance_eligibility_verification.domain.EligibilityEvents.EligibilityCheckFailed;
import com.dke.healthcare.insurance_eligibility_verification.domain.EligibilityEvents.EligibilityVerified;
import com.dke.healthcare.insurance_eligibility_verification.domain.EligibilityStatus;
import com.dke.healthcare.insurance_eligibility_verification.domain.Payer;
import com.dke.healthcare.insurance_eligibility_verification.integration.PayerGateway;
import com.dke.healthcare.insurance_eligibility_verification.integration.PayerGateway.EligibilityInquiry;
import com.dke.healthcare.insurance_eligibility_verification.integration.PayerGateway.PayerResponse;
import com.dke.healthcare.insurance_eligibility_verification.integration.PayerGateway.PayerUnavailableException;
import com.dke.healthcare.insurance_eligibility_verification.repository.CoverageRepository;
import com.dke.healthcare.insurance_eligibility_verification.repository.EligibilityCheckRepository;
import com.dke.healthcare.insurance_eligibility_verification.repository.PayerRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EligibilityService {

    static final int MAX_DAYS_AHEAD = 30;
    static final int MAX_DAYS_BACK = 365;
    static final Duration FRESHNESS = Duration.ofHours(24);

    public record VerificationResult(EligibilityCheck check, boolean reused) {
    }

    private final CoverageRepository coverages;
    private final PayerRepository payers;
    private final EligibilityCheckRepository checks;
    private final PayerGateway payerGateway;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public EligibilityService(CoverageRepository coverages, PayerRepository payers, EligibilityCheckRepository checks,
                              PayerGateway payerGateway, ApplicationEventPublisher events, Clock clock) {
        this.coverages = coverages;
        this.payers = payers;
        this.checks = checks;
        this.payerGateway = payerGateway;
        this.events = events;
        this.clock = clock;
    }

    /** Verify coverage for one date of service, reusing a fresh answer instead of paying for a second inquiry. */
    @Transactional
    public VerificationResult verify(String coverageId, LocalDate serviceDate, String serviceTypeCode) {
        Coverage coverage = coverages.findById(coverageId)
                .orElseThrow(() -> new NotFoundException("Coverage " + coverageId + " not found"));

        LocalDate today = LocalDate.now(clock);
        if (serviceDate.isAfter(today.plusDays(MAX_DAYS_AHEAD)) || serviceDate.isBefore(today.minusDays(MAX_DAYS_BACK))) {
            throw new EligibilityRuleException("Service date " + serviceDate + " is outside the inquiry window ("
                    + MAX_DAYS_BACK + " days back to " + MAX_DAYS_AHEAD + " days ahead)");
        }
        Payer payer = payers.findById(coverage.getPayerId())
                .orElseThrow(() -> new NotFoundException("Payer " + coverage.getPayerId() + " not found"));
        if (!payer.isRealTimeEnabled()) {
            throw new EligibilityRuleException("Payer " + payer.getName()
                    + " does not support real-time eligibility; verify by phone or payer portal");
        }

        Instant now = clock.instant();
        Optional<EligibilityCheck> fresh = checks
                .findFirstByCoverageIdAndServiceDateAndServiceTypeCodeAndStatusInAndCompletedAtAfterOrderByCompletedAtDesc(
                        coverageId, serviceDate, serviceTypeCode,
                        List.of(EligibilityStatus.ACTIVE, EligibilityStatus.INACTIVE), now.minus(FRESHNESS));
        if (fresh.isPresent()) {
            return new VerificationResult(fresh.get(), true);
        }

        EligibilityCheck check = new EligibilityCheck(UUID.randomUUID().toString(), coverageId, serviceDate, serviceTypeCode, now);
        submit(check, coverage);
        return new VerificationResult(checks.save(check), false);
    }

    /** Re-send an inquiry that failed because the payer was unreachable. */
    @Transactional
    public EligibilityCheck retry(String checkId) {
        EligibilityCheck check = get(checkId);
        check.resubmit(clock.instant());
        Coverage coverage = coverages.findById(check.getCoverageId())
                .orElseThrow(() -> new NotFoundException("Coverage " + check.getCoverageId() + " not found"));
        submit(check, coverage);
        return check;
    }

    @Transactional(readOnly = true)
    public EligibilityCheck get(String checkId) {
        return checks.findById(checkId)
                .orElseThrow(() -> new NotFoundException("Eligibility check " + checkId + " not found"));
    }

    @Transactional(readOnly = true)
    public List<EligibilityCheck> history(String coverageId) {
        return checks.findByCoverageIdOrderByRequestedAtDesc(coverageId);
    }

    private void submit(EligibilityCheck check, Coverage coverage) {
        Instant now = clock.instant();
        PayerResponse response;
        try {
            response = payerGateway.inquire(new EligibilityInquiry(coverage.getPayerId(), coverage.getMemberId(),
                    coverage.getSubscriberFirstName(), coverage.getSubscriberLastName(),
                    coverage.getSubscriberDateOfBirth(), check.getServiceDate(), check.getServiceTypeCode()));
        } catch (PayerUnavailableException e) {
            check.markPayerError(e.getMessage(), now);
            events.publishEvent(new EligibilityCheckFailed(check.getId(), coverage.getId(), check.getStatus(), e.getMessage()));
            return;
        }

        if (response.rejectReasonCode() != null) {
            check.markRejected(response.rejectReasonCode(), response.message(), now);
            events.publishEvent(new EligibilityCheckFailed(check.getId(), coverage.getId(), check.getStatus(),
                    "AAA " + response.rejectReasonCode() + ": " + response.message()));
        } else if (coveredOn(response, check.getServiceDate())) {
            check.markActive(response.planBeginDate(), response.planEndDate(),
                    response.copayAmount(), response.deductibleRemaining(), now);
            events.publishEvent(new EligibilityVerified(check.getId(), coverage.getId(),
                    check.getServiceDate(), check.getServiceTypeCode()));
        } else {
            String reason = response.message() != null ? response.message() : "No active coverage on " + check.getServiceDate();
            check.markInactive(response.planBeginDate(), response.planEndDate(), reason, now);
            events.publishEvent(new CoverageInactive(check.getId(), coverage.getId(), check.getServiceDate(), reason));
        }
    }

    /** "Active" alone is not enough: the date of service must also fall inside the plan dates. */
    static boolean coveredOn(PayerResponse response, LocalDate serviceDate) {
        return response.activeCoverage()
                && response.planBeginDate() != null && !serviceDate.isBefore(response.planBeginDate())
                && (response.planEndDate() == null || !serviceDate.isAfter(response.planEndDate()));
    }
}
