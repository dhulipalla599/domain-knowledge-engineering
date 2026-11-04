package com.dke.capital_markets.brokerage_account_opening.integration;

import java.time.LocalDate;

/** Port for the identity-verification vendor. Production would call a KYC provider over HTTPS. */
public interface IdentityVerifier {

    boolean verify(String applicantName, LocalDate dateOfBirth, String taxIdLast4);
}
