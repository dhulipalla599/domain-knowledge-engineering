package com.dke.insurance.underwriting.integration;

import java.util.Map;
import org.springframework.stereotype.Component;

/** Deterministic stand-in so the example needs no bureau account. */
@Component
public class StubClaimsHistoryClient implements ClaimsHistoryClient {

    private static final Map<String, Integer> KNOWN = Map.of("Marcus Webb", 4, "Priya Nair", 1);

    @Override
    public int claimsInLastFiveYears(String applicantName) {
        return KNOWN.getOrDefault(applicantName, 0);
    }
}
