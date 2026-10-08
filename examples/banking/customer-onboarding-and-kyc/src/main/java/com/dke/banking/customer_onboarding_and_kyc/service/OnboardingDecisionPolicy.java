package com.dke.banking.customer_onboarding_and_kyc.service;

import com.dke.banking.customer_onboarding_and_kyc.domain.Decision;
import com.dke.banking.customer_onboarding_and_kyc.domain.RiskRating;
import com.dke.banking.customer_onboarding_and_kyc.domain.ScreeningOutcome;
import org.springframework.stereotype.Component;

/**
 * The key business rule: when may an application be approved automatically?
 * <ul>
 *   <li>A confirmed sanctions match is rejected.</li>
 *   <li>A possible match is never auto-approved; a human must clear it.</li>
 *   <li>High-risk customers need enhanced due diligence (EDD), so they go to review.</li>
 *   <li>Everything else is approved.</li>
 * </ul>
 */
@Component
public class OnboardingDecisionPolicy {

    public Decision decide(ScreeningOutcome screening, RiskRating rating) {
        if (screening == ScreeningOutcome.CONFIRMED_MATCH) {
            return Decision.REJECT;
        }
        if (screening == ScreeningOutcome.POSSIBLE_MATCH) {
            return Decision.REVIEW;
        }
        if (rating == RiskRating.HIGH) {
            return Decision.REVIEW;
        }
        return Decision.APPROVE;
    }
}
