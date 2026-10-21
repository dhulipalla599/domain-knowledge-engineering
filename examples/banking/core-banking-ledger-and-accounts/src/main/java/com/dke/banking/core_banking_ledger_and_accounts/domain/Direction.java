package com.dke.banking.core_banking_ledger_and_accounts.domain;

/** Customer accounts are liabilities of the bank: a debit reduces the balance, a credit increases it. */
public enum Direction {
    DEBIT, CREDIT;

    public Direction opposite() {
        return this == DEBIT ? CREDIT : DEBIT;
    }
}
