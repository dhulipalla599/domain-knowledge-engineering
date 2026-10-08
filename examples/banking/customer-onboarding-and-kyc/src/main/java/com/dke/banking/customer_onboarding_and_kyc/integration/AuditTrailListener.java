package com.dke.banking.customer_onboarding_and_kyc.integration;

import com.dke.banking.customer_onboarding_and_kyc.domain.OnboardingEvents.ApplicationApproved;
import com.dke.banking.customer_onboarding_and_kyc.domain.OnboardingEvents.ApplicationRejected;
import com.dke.banking.customer_onboarding_and_kyc.domain.OnboardingEvents.ApplicationSubmitted;
import com.dke.banking.customer_onboarding_and_kyc.domain.OnboardingEvents.ScreeningCompleted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Stand-in for the audit consumer: every decision is recorded with its inputs and time. */
@Component
public class AuditTrailListener {

    private static final Logger log = LoggerFactory.getLogger("audit");

    @EventListener
    public void on(ApplicationSubmitted e) {
        log.info("application.submitted id={} at={}", e.applicationId(), e.at());
    }

    @EventListener
    public void on(ScreeningCompleted e) {
        log.info("screening.completed id={} outcome={} rating={}", e.applicationId(), e.outcome(), e.rating());
    }

    @EventListener
    public void on(ApplicationApproved e) {
        log.info("application.approved id={} rating={}", e.applicationId(), e.rating());
    }

    @EventListener
    public void on(ApplicationRejected e) {
        log.info("application.rejected id={} reason={}", e.applicationId(), e.internalReason());
    }
}
