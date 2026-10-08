package com.dke.banking.customer_onboarding_and_kyc.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dke.banking.customer_onboarding_and_kyc.domain.Decision;
import com.dke.banking.customer_onboarding_and_kyc.domain.RiskRating;
import com.dke.banking.customer_onboarding_and_kyc.domain.ScreeningOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class OnboardingDecisionPolicyTest {

    private final OnboardingDecisionPolicy policy = new OnboardingDecisionPolicy();
    private final RiskRatingPolicy risk = new RiskRatingPolicy();

    @ParameterizedTest
    @CsvSource({
            "CLEAR,           LOW,    APPROVE",
            "CLEAR,           MEDIUM, APPROVE",
            "CLEAR,           HIGH,   REVIEW",
            "POSSIBLE_MATCH,  LOW,    REVIEW",
            "CONFIRMED_MATCH, LOW,    REJECT",
            "CONFIRMED_MATCH, HIGH,   REJECT"
    })
    void decidesFromScreeningAndRisk(ScreeningOutcome screening, RiskRating rating, Decision expected) {
        assertThat(policy.decide(screening, rating)).isEqualTo(expected);
    }

    @Test
    void possibleMatchIsNeverAutoApproved() {
        for (RiskRating rating : RiskRating.values()) {
            assertThat(policy.decide(ScreeningOutcome.POSSIBLE_MATCH, rating)).isNotEqualTo(Decision.APPROVE);
        }
    }

    @Test
    void ratesRiskFromCountryAndOccupation() {
        assertThat(risk.rate("GB", "Software engineer")).isEqualTo(RiskRating.LOW);
        assertThat(risk.rate("GB", "Casino manager")).isEqualTo(RiskRating.MEDIUM);
        assertThat(risk.rate("XA", "Car dealer")).isEqualTo(RiskRating.HIGH);
    }
}
