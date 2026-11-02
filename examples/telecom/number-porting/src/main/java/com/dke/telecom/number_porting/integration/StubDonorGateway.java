package com.dke.telecom.number_porting.integration;

import java.util.Map;
import org.springframework.stereotype.Component;

/** Deterministic stand-in: the donor "knows" a few numbers, one of which is under contract lock. */
@Component
public class StubDonorGateway implements DonorGateway {

    private static final Map<String, String> OWNERS = Map.of(
            "+15550101001", "Ada Lovelace",
            "+15550101002", "Grace Hopper",
            "+15550101003", "Alan Turing");
    private static final String CONTRACT_LOCKED = "+15550101003";

    @Override
    public Decision validate(String msisdn, String subscriberName, String donorOperator) {
        String owner = OWNERS.get(msisdn);
        if (owner == null) {
            return Decision.reject("NUMBER_NOT_FOUND_AT_DONOR");
        }
        if (!owner.equalsIgnoreCase(subscriberName)) {
            return Decision.reject("SUBSCRIBER_NAME_MISMATCH");
        }
        if (CONTRACT_LOCKED.equals(msisdn)) {
            return Decision.reject("CONTRACT_LOCK_ACTIVE");
        }
        return Decision.accept();
    }
}
