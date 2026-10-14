package com.dke.insurance.quote_and_rating.integration;

import org.springframework.stereotype.Component;

/** Deterministic stand-in: the same name always gets the same score between 500 and 849. */
@Component
public class StubCreditScoreProvider implements CreditScoreProvider {

    @Override
    public int scoreFor(String driverName) {
        return 500 + Math.floorMod(driverName.toLowerCase().hashCode(), 350);
    }
}
