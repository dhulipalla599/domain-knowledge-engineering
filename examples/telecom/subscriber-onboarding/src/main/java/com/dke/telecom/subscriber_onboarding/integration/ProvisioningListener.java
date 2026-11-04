package com.dke.telecom.subscriber_onboarding.integration;

import com.dke.telecom.subscriber_onboarding.domain.OnboardingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Stands in for the network-provisioning and billing consumers of subscription.activated. */
@Component
public class ProvisioningListener {

    private static final Logger log = LoggerFactory.getLogger(ProvisioningListener.class);

    @EventListener
    public void onActivated(OnboardingEvent.Activated event) {
        log.info("[{}] provision {} in HSS and open {} billing account for {}",
                event.topic(), event.msisdn(), event.plan(), event.subscriptionId());
    }
}
