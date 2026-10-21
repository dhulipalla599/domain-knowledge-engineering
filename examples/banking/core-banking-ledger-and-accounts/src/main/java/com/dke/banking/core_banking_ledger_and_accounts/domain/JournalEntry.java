package com.dke.banking.core_banking_ledger_and_accounts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A balanced set of ledger lines that moves money atomically. The unique idempotency key makes retries safe. */
@Entity
@Table(name = "journal_entries")
public class JournalEntry {

    @Id
    private UUID id;
    @Column(unique = true, nullable = false)
    private String idempotencyKey;
    private String reference;
    private String currency;
    private long amountMinor;
    private UUID reversalOf;
    private Instant createdAt;
    @Enumerated(EnumType.STRING)
    private EntryStatus status;

    protected JournalEntry() {
    }

    public JournalEntry(UUID id, String idempotencyKey, String reference, String currency, long amountMinor,
                        UUID reversalOf, Instant createdAt) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.reference = reference;
        this.currency = currency;
        this.amountMinor = amountMinor;
        this.reversalOf = reversalOf;
        this.createdAt = createdAt;
        this.status = EntryStatus.PENDING;
    }

    /** Double-entry invariant: total debits must equal total credits. */
    public static void requireBalanced(List<LedgerLine> lines) {
        long debits = lines.stream().filter(l -> l.getDirection() == Direction.DEBIT).mapToLong(LedgerLine::getAmountMinor).sum();
        long credits = lines.stream().filter(l -> l.getDirection() == Direction.CREDIT).mapToLong(LedgerLine::getAmountMinor).sum();
        if (debits != credits) {
            throw new BusinessRuleViolation("UNBALANCED_ENTRY", "Debits " + debits + " do not equal credits " + credits);
        }
    }

    public void post() {
        moveTo(EntryStatus.POSTED);
    }

    public void markReversed() {
        moveTo(EntryStatus.REVERSED);
    }

    private void moveTo(EntryStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new BusinessRuleViolation("ILLEGAL_ENTRY_TRANSITION", "Entry " + id + " is " + status + ", cannot become " + next);
        }
        this.status = next;
    }

    public boolean isReversal() { return reversalOf != null; }
    public UUID getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getReference() { return reference; }
    public String getCurrency() { return currency; }
    public long getAmountMinor() { return amountMinor; }
    public UUID getReversalOf() { return reversalOf; }
    public Instant getCreatedAt() { return createdAt; }
    public EntryStatus getStatus() { return status; }
}
