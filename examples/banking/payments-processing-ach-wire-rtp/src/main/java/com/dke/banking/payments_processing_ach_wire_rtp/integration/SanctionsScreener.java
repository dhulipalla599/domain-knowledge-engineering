package com.dke.banking.payments_processing_ach_wire_rtp.integration;

/** Port to a sanctions screening provider; production would call a vendor or an in-house list service. */
public interface SanctionsScreener {

    boolean isBlocked(String partyName);
}
