package com.dke.banking.core_banking_ledger_and_accounts.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** One side of a journal entry. Lines are append-only: they are never updated or deleted. */
@Entity
@Table(name = "ledger_lines")
public class LedgerLine {

    @Id
    private UUID id;
    private UUID entryId;
    private String accountNumber;
    @Enumerated(EnumType.STRING)
    private Direction direction;
    private long amountMinor;
    private Instant postedAt;

    protected LedgerLine() {
    }

    public LedgerLine(UUID entryId, String accountNumber, Direction direction, long amountMinor, Instant postedAt) {
        this.id = UUID.randomUUID();
        this.entryId = entryId;
        this.accountNumber = accountNumber;
        this.direction = direction;
        this.amountMinor = amountMinor;
        this.postedAt = postedAt;
    }

    public UUID getEntryId() { return entryId; }
    public String getAccountNumber() { return accountNumber; }
    public Direction getDirection() { return direction; }
    public long getAmountMinor() { return amountMinor; }
    public Instant getPostedAt() { return postedAt; }
}
