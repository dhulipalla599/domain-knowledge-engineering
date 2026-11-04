package com.dke.banking.payments_processing_ach_wire_rtp.integration;

import java.util.Locale;
import org.springframework.stereotype.Component;

/** Deterministic stand-in: any name containing "BLOCKED" is a hit. */
@Component
public class StubSanctionsScreener implements SanctionsScreener {

    @Override
    public boolean isBlocked(String partyName) {
        return partyName.toUpperCase(Locale.ROOT).contains("BLOCKED");
    }
}
