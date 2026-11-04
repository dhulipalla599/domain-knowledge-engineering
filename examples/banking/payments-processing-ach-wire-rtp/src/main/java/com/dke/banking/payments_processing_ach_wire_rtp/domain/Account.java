package com.dke.banking.payments_processing_ach_wire_rtp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;

/** Customer account: money is first held, then settled or released. */
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @Column(name = "account_number")
    private String accountNumber;
    @Column(name = "owner_name", nullable = false)
    private String ownerName;
    @Column(nullable = false)
    private BigDecimal balance;
    @Column(nullable = false)
    private BigDecimal held;
    @Version
    private long version;   // optimistic lock: two payments cannot both spend the same money

    protected Account() {
    }

    public Account(String accountNumber, String ownerName, BigDecimal balance) {
        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
        this.balance = balance;
        this.held = BigDecimal.ZERO;
    }

    public BigDecimal available() {
        return balance.subtract(held);
    }

    public void hold(BigDecimal amount) {
        if (available().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient available balance");
        }
        held = held.add(amount);
    }

    public void release(BigDecimal amount) {
        held = held.subtract(amount);
    }

    public void settle(BigDecimal amount) {
        held = held.subtract(amount);
        balance = balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        balance = balance.add(amount);
    }

    public String getAccountNumber() { return accountNumber; }
    public String getOwnerName() { return ownerName; }
    public BigDecimal getBalance() { return balance; }
    public BigDecimal getHeld() { return held; }
}
