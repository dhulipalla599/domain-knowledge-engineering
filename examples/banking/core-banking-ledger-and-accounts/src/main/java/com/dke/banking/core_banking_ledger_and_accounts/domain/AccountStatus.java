package com.dke.banking.core_banking_ledger_and_accounts.domain;

/** Account lifecycle: ACTIVE <-> FROZEN, and either can be CLOSED (terminal). */
public enum AccountStatus {
    ACTIVE, FROZEN, CLOSED;

    public boolean canTransitionTo(AccountStatus next) {
        return switch (this) {
            case ACTIVE -> next == FROZEN || next == CLOSED;
            case FROZEN -> next == ACTIVE || next == CLOSED;
            case CLOSED -> false;
        };
    }
}
