package com.dke.insurance.quote_and_rating.integration;

/** Port for a credit-based insurance score vendor. Production: an HTTPS adapter with timeouts and a circuit breaker. */
public interface CreditScoreProvider {

    int scoreFor(String driverName);
}
