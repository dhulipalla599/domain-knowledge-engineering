package com.dke.banking.customer_onboarding_and_kyc.integration;

import com.dke.banking.customer_onboarding_and_kyc.domain.ScreeningOutcome;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Deterministic stand-in for a screening vendor, using a tiny fictional watchlist.
 * Same name and date of birth = confirmed match; same name only = possible match.
 * A real adapter would call the vendor's API and store its reference for the audit trail.
 */
@Component
public class StubSanctionsScreeningClient implements SanctionsScreeningClient {

    private static final Map<String, LocalDate> WATCHLIST = Map.of(
            "viktor petrov", LocalDate.of(1970, 1, 1),
            "ivan drago", LocalDate.of(1962, 7, 4));

    @Override
    public ScreeningOutcome screen(String legalName, LocalDate dateOfBirth) {
        LocalDate listed = WATCHLIST.get(normalize(legalName));
        if (listed == null) {
            return ScreeningOutcome.CLEAR;
        }
        return listed.equals(dateOfBirth) ? ScreeningOutcome.CONFIRMED_MATCH : ScreeningOutcome.POSSIBLE_MATCH;
    }

    static String normalize(String name) {
        String ascii = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return ascii.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
