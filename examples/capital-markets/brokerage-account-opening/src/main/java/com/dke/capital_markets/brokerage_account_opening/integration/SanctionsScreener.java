package com.dke.capital_markets.brokerage_account_opening.integration;

/** Port for sanctions / watch-list screening. A possible match needs a human, it is never auto-rejected. */
public interface SanctionsScreener {

    boolean possibleMatch(String applicantName);
}
