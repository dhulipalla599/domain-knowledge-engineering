package com.dke.healthcare.insurance_eligibility_verification.integration;

import com.dke.healthcare.insurance_eligibility_verification.domain.EligibilityEvents.CoverageInactive;
import com.dke.healthcare.insurance_eligibility_verification.domain.EligibilityEvents.EligibilityCheckFailed;
import com.dke.healthcare.insurance_eligibility_verification.domain.EligibilityEvents.EligibilityVerified;
import com.dke.healthcare.insurance_eligibility_verification.domain.EligibilityStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Stand-ins for the Kafka consumers: registration worklist, financial counseling and retry scheduler. */
@Component
public class EligibilityEventListener {

    private static final Logger log = LoggerFactory.getLogger(EligibilityEventListener.class);

    @EventListener
    void onVerified(EligibilityVerified e) {
        log.info("[eligibility.verified] clear registration worklist item for coverage {} on {}", e.coverageId(), e.serviceDate());
    }

    @EventListener
    void onInactive(CoverageInactive e) {
        log.info("[eligibility.coverage-inactive] route coverage {} to financial counseling: {}", e.coverageId(), e.reason());
    }

    @EventListener
    void onFailed(EligibilityCheckFailed e) {
        if (e.status() == EligibilityStatus.PAYER_ERROR) {
            log.info("[eligibility.check-failed] schedule retry of check {}: {}", e.checkId(), e.reason());
        } else {
            log.info("[eligibility.check-failed] ask front desk to correct coverage {}: {}", e.coverageId(), e.reason());
        }
    }
}
