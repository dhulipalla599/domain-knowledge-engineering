package com.dke.banking.core_banking_ledger_and_accounts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;

/** A customer account. Balances are stored in minor units (pence, cents) as whole numbers. */
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    private UUID id;
    @Column(unique = true, nullable = false)
    private String number;
    private String ownerName;
    private String currency;
    private long balanceMinor;
    private long overdraftLimitMinor;
    @Enumerated(EnumType.STRING)
    private AccountStatus status;
    @Version
    private long version;

    protected Account() {
    }

    public Account(String number, String ownerName, String currency, long balanceMinor, long overdraftLimitMinor) {
        this.id = UUID.randomUUID();
        this.number = number;
        this.ownerName = ownerName;
        this.currency = currency;
        this.balanceMinor = balanceMinor;
        this.overdraftLimitMinor = overdraftLimitMinor;
        this.status = AccountStatus.ACTIVE;
    }

    public void assertActive() {
        if (status != AccountStatus.ACTIVE) {
            throw new BusinessRuleViolation("ACCOUNT_NOT_ACTIVE", "Account " + number + " is " + status);
        }
    }

    /** Key rule: a debit may not take the balance below the agreed overdraft limit. */
    public void debit(long amountMinor, boolean enforceLimit) {
        if (enforceLimit && balanceMinor - amountMinor < -overdraftLimitMinor) {
            throw new BusinessRuleViolation("INSUFFICIENT_FUNDS",
                    "Account " + number + " has " + availableMinor() + " available, needs " + amountMinor);
        }
        balanceMinor -= amountMinor;
    }

    public void credit(long amountMinor) {
        balanceMinor += amountMinor;
    }

    public void apply(Direction direction, long amountMinor, boolean enforceLimit) {
        if (direction == Direction.DEBIT) {
            debit(amountMinor, enforceLimit);
        } else {
            credit(amountMinor);
        }
    }

    public void transitionTo(AccountStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new BusinessRuleViolation("ILLEGAL_ACCOUNT_TRANSITION", status + " -> " + next + " is not allowed");
        }
        if (next == AccountStatus.CLOSED && balanceMinor != 0) {
            throw new BusinessRuleViolation("NON_ZERO_BALANCE", "Account " + number + " must have a zero balance to close");
        }
        this.status = next;
    }

    public long availableMinor() {
        return balanceMinor + overdraftLimitMinor;
    }

    public UUID getId() { return id; }
    public String getNumber() { return number; }
    public String getOwnerName() { return ownerName; }
    public String getCurrency() { return currency; }
    public long getBalanceMinor() { return balanceMinor; }
    public long getOverdraftLimitMinor() { return overdraftLimitMinor; }
    public AccountStatus getStatus() { return status; }
}
