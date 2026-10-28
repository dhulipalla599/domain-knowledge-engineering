package com.dke.insurance.underwriting.integration;

/** Port to an external claims-history bureau. Production: an HTTP adapter with timeout and circuit breaker. */
public interface ClaimsHistoryClient {

    int claimsInLastFiveYears(String applicantName);
}
