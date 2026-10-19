package com.dke.telecom.subscriber_onboarding.integration;

import com.dke.telecom.subscriber_onboarding.domain.OnboardingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Stands in for the SMS/email notification service. */
@Component
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    @EventListener
    public void onActivated(OnboardingEvent.Activated event) {
        log.info("[{}] welcome SMS to {}", event.topic(), event.msisdn());
    }

    @EventListener
    public void onRejected(OnboardingEvent.Rejected event) {
        log.info("[{}] rejection notice for {}: {}", event.topic(), event.subscriptionId(), event.reason());
    }
}
