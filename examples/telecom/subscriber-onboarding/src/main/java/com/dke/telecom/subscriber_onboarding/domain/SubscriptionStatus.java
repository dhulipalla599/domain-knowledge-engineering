package com.dke.telecom.subscriber_onboarding.domain;

import java.util.EnumSet;
import java.util.Set;

public enum SubscriptionStatus {
    SUBMITTED, VERIFIED, ACTIVE, REJECTED, CANCELLED;

    public Set<SubscriptionStatus> allowedNext() {
        return switch (this) {
            case SUBMITTED -> EnumSet.of(VERIFIED, REJECTED);
            case VERIFIED -> EnumSet.of(ACTIVE, CANCELLED);
            case ACTIVE -> EnumSet.of(CANCELLED);
            case REJECTED, CANCELLED -> EnumSet.noneOf(SubscriptionStatus.class);
        };
    }
}
