package com.dke.banking.core_banking_ledger_and_accounts.integration;

import org.springframework.stereotype.Component;

/** Deterministic stand-in: holds any single transfer above 50,000.00. */
@Component
public class StubFraudScreeningClient implements FraudScreeningClient {

    static final long HOLD_THRESHOLD_MINOR = 5_000_000L;

    @Override
    public boolean isSuspicious(String fromAccount, String toAccount, long amountMinor, String currency) {
        return amountMinor > HOLD_THRESHOLD_MINOR;
    }
}
