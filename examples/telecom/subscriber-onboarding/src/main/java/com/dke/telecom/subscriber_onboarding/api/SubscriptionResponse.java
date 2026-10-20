package com.dke.telecom.subscriber_onboarding.api;

import com.dke.telecom.subscriber_onboarding.domain.Plan;
import com.dke.telecom.subscriber_onboarding.domain.Subscription;
import com.dke.telecom.subscriber_onboarding.domain.SubscriptionStatus;
import java.time.Instant;
import java.util.UUID;

public record SubscriptionResponse(UUID id, SubscriptionStatus status, Plan plan, String iccid,
                                   String msisdn, Instant activatedAt) {

    static SubscriptionResponse from(Subscription s) {
        return new SubscriptionResponse(s.getId(), s.getStatus(), s.getPlan(), s.getIccid(),
                s.getMsisdn(), s.getActivatedAt());
    }
}
