package com.dke.banking.core_banking_ledger_and_accounts.domain;

/** Journal entry lifecycle. POSTED entries are immutable; a correction is a new, opposite entry. */
public enum EntryStatus {
    PENDING, POSTED, REVERSED;

    public boolean canTransitionTo(EntryStatus next) {
        return switch (this) {
            case PENDING -> next == POSTED;
            case POSTED -> next == REVERSED;
            case REVERSED -> false;
        };
    }
}
