package com.dke.telecom.subscriber_onboarding.domain;

import java.util.UUID;

/** In-process stand-ins for Apache Kafka messages; topic() names the topic each one would use. */
public sealed interface OnboardingEvent {

    UUID subscriptionId();

    String topic();

    record Submitted(UUID subscriptionId, String iccid) implements OnboardingEvent {
        public String topic() { return "subscription.submitted"; }
    }

    record Activated(UUID subscriptionId, String msisdn, Plan plan) implements OnboardingEvent {
        public String topic() { return "subscription.activated"; }
    }

    record Rejected(UUID subscriptionId, String reason) implements OnboardingEvent {
        public String topic() { return "subscription.rejected"; }
    }
}
