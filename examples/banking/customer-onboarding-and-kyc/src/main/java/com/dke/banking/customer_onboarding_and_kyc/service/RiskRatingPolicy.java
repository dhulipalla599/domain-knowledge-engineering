package com.dke.banking.customer_onboarding_and_kyc.service;

import com.dke.banking.customer_onboarding_and_kyc.domain.RiskRating;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Simplified customer risk rating. Real banks use a documented, versioned model with
 * many more factors (product, channel, source of funds, ownership structure).
 */
@Component
public class RiskRatingPolicy {

    /** Fictional list for the example; real lists come from the compliance team. */
    static final Set<String> HIGH_RISK_COUNTRIES = Set.of("XA", "XB");
    static final Set<String> CASH_INTENSIVE_OCCUPATIONS = Set.of("casino", "money service", "car dealer");

    public RiskRating rate(String nationality, String occupation) {
        String job = occupation.toLowerCase(Locale.ROOT);
        boolean highRiskCountry = HIGH_RISK_COUNTRIES.contains(nationality.toUpperCase(Locale.ROOT));
        boolean cashIntensive = CASH_INTENSIVE_OCCUPATIONS.stream().anyMatch(job::contains);
        if (highRiskCountry && cashIntensive) {
            return RiskRating.HIGH;
        }
        if (highRiskCountry || cashIntensive) {
            return RiskRating.MEDIUM;
        }
        return RiskRating.LOW;
    }
}
