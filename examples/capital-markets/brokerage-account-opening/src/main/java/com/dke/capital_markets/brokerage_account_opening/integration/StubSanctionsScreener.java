package com.dke.capital_markets.brokerage_account_opening.integration;

import org.springframework.stereotype.Component;

/** Deterministic stand-in: names containing "Blocklisted" count as a possible match. */
@Component
public class StubSanctionsScreener implements SanctionsScreener {

    @Override
    public boolean possibleMatch(String applicantName) {
        return applicantName.toLowerCase().contains("blocklisted");
    }
}
