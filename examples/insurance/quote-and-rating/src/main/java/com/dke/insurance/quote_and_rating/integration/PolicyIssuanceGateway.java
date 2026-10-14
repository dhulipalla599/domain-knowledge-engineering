package com.dke.insurance.quote_and_rating.integration;

import java.util.UUID;

/** Port for the policy administration system. Production: consumer of insurance.quote.bound that issues the policy. */
public interface PolicyIssuanceGateway {

    String issuePolicy(UUID quoteId, String driverName);
}
