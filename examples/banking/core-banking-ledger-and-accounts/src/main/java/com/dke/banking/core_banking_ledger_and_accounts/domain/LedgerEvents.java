package com.dke.banking.core_banking_ledger_and_accounts.domain;

import java.time.Instant;
import java.util.UUID;

/** Domain events. Each maps to a Kafka topic in production; keyed by account number to keep per-account order. */
public final class LedgerEvents {

    private LedgerEvents() {
    }

    /** Topic: ledger.entry.posted */
    public record EntryPosted(UUID entryId, String fromAccount, String toAccount, long amountMinor, String currency,
                              Instant postedAt) {
    }

    /** Topic: ledger.entry.reversed */
    public record EntryReversed(UUID originalEntryId, UUID reversalEntryId, Instant reversedAt) {
    }

    /** Topic: ledger.account.status-changed */
    public record AccountStatusChanged(String accountNumber, AccountStatus from, AccountStatus to) {
    }
}
