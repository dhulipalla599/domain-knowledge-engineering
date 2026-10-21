package com.dke.banking.core_banking_ledger_and_accounts.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Plain unit tests of the ledger rules: no Spring, no database. */
class AccountTest {

    @Test
    void debitWithinOverdraftLimitIsAllowed() {
        Account account = new Account("ACC-T1", "Test", "GBP", 1_000, 500);
        account.debit(1_500, true);
        assertThat(account.getBalanceMinor()).isEqualTo(-500);
    }

    @Test
    void debitBeyondOverdraftLimitIsRejectedAndBalanceUnchanged() {
        Account account = new Account("ACC-T2", "Test", "GBP", 1_000, 500);
        assertThatThrownBy(() -> account.debit(1_501, true))
                .isInstanceOf(BusinessRuleViolation.class)
                .extracting(e -> ((BusinessRuleViolation) e).getCode()).isEqualTo("INSUFFICIENT_FUNDS");
        assertThat(account.getBalanceMinor()).isEqualTo(1_000);
    }

    @Test
    void closingRequiresZeroBalanceAndClosedIsTerminal() {
        Account account = new Account("ACC-T3", "Test", "GBP", 10, 0);
        assertThatThrownBy(() -> account.transitionTo(AccountStatus.CLOSED)).hasMessageContaining("zero balance");
        account.debit(10, true);
        account.transitionTo(AccountStatus.CLOSED);
        assertThatThrownBy(() -> account.transitionTo(AccountStatus.ACTIVE))
                .isInstanceOf(BusinessRuleViolation.class);
    }

    @Test
    void unbalancedEntryIsRejected() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        List<LedgerLine> lines = List.of(
                new LedgerLine(id, "A", Direction.DEBIT, 100, now),
                new LedgerLine(id, "B", Direction.CREDIT, 90, now));
        assertThatThrownBy(() -> JournalEntry.requireBalanced(lines))
                .isInstanceOf(BusinessRuleViolation.class).hasMessageContaining("do not equal");
    }

    @Test
    void postedEntryCanBeReversedOnlyOnce() {
        JournalEntry entry = new JournalEntry(UUID.randomUUID(), "k", "ref", "GBP", 100, null, Instant.now());
        entry.post();
        entry.markReversed();
        assertThat(entry.getStatus()).isEqualTo(EntryStatus.REVERSED);
        assertThatThrownBy(entry::markReversed).isInstanceOf(BusinessRuleViolation.class);
    }
}
