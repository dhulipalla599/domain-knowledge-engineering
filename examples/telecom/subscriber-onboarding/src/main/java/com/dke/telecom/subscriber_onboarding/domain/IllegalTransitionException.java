package com.dke.telecom.subscriber_onboarding.domain;

public class IllegalTransitionException extends RuntimeException {
    public IllegalTransitionException(SubscriptionStatus from, SubscriptionStatus to) {
        super("Cannot move a subscription from " + from + " to " + to);
    }
}
