package com.dke.capital_markets.brokerage_account_opening.integration;

import java.time.LocalDate;
import org.springframework.stereotype.Component;

/** Deterministic stand-in: any tax id ending in 0000 fails verification. */
@Component
public class StubIdentityVerifier implements IdentityVerifier {

    @Override
    public boolean verify(String applicantName, LocalDate dateOfBirth, String taxIdLast4) {
        return !"0000".equals(taxIdLast4);
    }
}
