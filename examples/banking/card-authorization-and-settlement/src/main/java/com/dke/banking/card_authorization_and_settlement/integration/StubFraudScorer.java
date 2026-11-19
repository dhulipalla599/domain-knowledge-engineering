package com.dke.banking.card_authorization_and_settlement.integration;

import java.math.BigDecimal;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Deterministic stand-in for a fraud model: high-risk merchant categories with a large
 * amount score high, everything else scores low.
 */
@Component
public class StubFraudScorer implements FraudScorer {

    private static final Set<String> HIGH_RISK_MCC = Set.of("6051", "7995"); // quasi-cash, gambling
    private static final BigDecimal LARGE = new BigDecimal("1000.00");

    @Override
    public int score(String cardToken, String mcc, BigDecimal amount) {
        boolean risky = HIGH_RISK_MCC.contains(mcc);
        boolean large = amount.compareTo(LARGE) >= 0;
        return risky && large ? 950 : risky ? 600 : large ? 300 : 50;
    }
}
