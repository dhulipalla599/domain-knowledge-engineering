package com.dke.banking.core_banking_ledger_and_accounts.integration;

/** Port for the real-time fraud engine. Production: a gRPC/REST call to the fraud service with a tight timeout. */
public interface FraudScreeningClient {

    boolean isSuspicious(String fromAccount, String toAccount, long amountMinor, String currency);
}
