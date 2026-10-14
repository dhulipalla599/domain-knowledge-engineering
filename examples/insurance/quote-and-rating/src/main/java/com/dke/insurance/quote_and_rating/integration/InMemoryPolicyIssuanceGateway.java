package com.dke.insurance.quote_and_rating.integration;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Issues a policy number per quote. Re-delivery of the same event returns the same number (idempotent consumer). */
@Component
public class InMemoryPolicyIssuanceGateway implements PolicyIssuanceGateway {

    private final Map<UUID, String> issued = new ConcurrentHashMap<>();

    @Override
    public String issuePolicy(UUID quoteId, String driverName) {
        return issued.computeIfAbsent(quoteId, id -> "POL-" + id.toString().substring(0, 8).toUpperCase());
    }
}
