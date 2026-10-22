package com.dke.banking.core_banking_ledger_and_accounts.service;

import com.dke.banking.core_banking_ledger_and_accounts.domain.Account;
import com.dke.banking.core_banking_ledger_and_accounts.domain.AccountStatus;
import com.dke.banking.core_banking_ledger_and_accounts.domain.BusinessRuleViolation;
import com.dke.banking.core_banking_ledger_and_accounts.domain.Direction;
import com.dke.banking.core_banking_ledger_and_accounts.domain.EntryStatus;
import com.dke.banking.core_banking_ledger_and_accounts.domain.JournalEntry;
import com.dke.banking.core_banking_ledger_and_accounts.domain.LedgerEvents.AccountStatusChanged;
import com.dke.banking.core_banking_ledger_and_accounts.domain.LedgerEvents.EntryPosted;
import com.dke.banking.core_banking_ledger_and_accounts.domain.LedgerEvents.EntryReversed;
import com.dke.banking.core_banking_ledger_and_accounts.domain.LedgerLine;
import com.dke.banking.core_banking_ledger_and_accounts.integration.FraudScreeningClient;
import com.dke.banking.core_banking_ledger_and_accounts.repository.AccountRepository;
import com.dke.banking.core_banking_ledger_and_accounts.repository.JournalEntryRepository;
import com.dke.banking.core_banking_ledger_and_accounts.repository.LedgerLineRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerService {

    private final AccountRepository accounts;
    private final JournalEntryRepository entries;
    private final LedgerLineRepository lines;
    private final FraudScreeningClient fraud;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public LedgerService(AccountRepository accounts, JournalEntryRepository entries, LedgerLineRepository lines,
                         FraudScreeningClient fraud, ApplicationEventPublisher events, Clock clock) {
        this.accounts = accounts;
        this.entries = entries;
        this.lines = lines;
        this.fraud = fraud;
        this.events = events;
        this.clock = clock;
    }

    /** Moves money between two accounts as one balanced, atomic journal entry. Safe to retry with the same key. */
    @Transactional
    public JournalEntry transfer(String idempotencyKey, String from, String to, long amountMinor, String currency,
                                 String reference) {
        var replay = entries.findByIdempotencyKey(idempotencyKey);
        if (replay.isPresent()) {
            return replay.get();
        }
        if (amountMinor <= 0) {
            throw new BusinessRuleViolation("INVALID_AMOUNT", "Amount must be positive");
        }
        if (from.equals(to)) {
            throw new BusinessRuleViolation("SAME_ACCOUNT", "Source and destination must differ");
        }
        // Lock in a fixed order so two opposite transfers can never deadlock each other.
        Account source = null;
        Account target = null;
        for (String number : List.of(from, to).stream().sorted().toList()) {
            Account locked = accounts.findByNumberForUpdate(number)
                    .orElseThrow(() -> new NotFoundException("Account " + number + " not found"));
            if (number.equals(from)) source = locked; else target = locked;
        }
        source.assertActive();
        target.assertActive();
        if (!source.getCurrency().equals(currency) || !target.getCurrency().equals(currency)) {
            throw new BusinessRuleViolation("CURRENCY_MISMATCH", "Both accounts must be held in " + currency);
        }
        if (fraud.isSuspicious(from, to, amountMinor, currency)) {
            throw new BusinessRuleViolation("FRAUD_HOLD", "Transfer held for fraud review");
        }

        Instant now = clock.instant();
        UUID entryId = UUID.randomUUID();
        JournalEntry entry = new JournalEntry(entryId, idempotencyKey, reference, currency, amountMinor, null, now);
        List<LedgerLine> posting = List.of(
                new LedgerLine(entryId, from, Direction.DEBIT, amountMinor, now),
                new LedgerLine(entryId, to, Direction.CREDIT, amountMinor, now));
        JournalEntry.requireBalanced(posting);

        source.debit(amountMinor, true);
        target.credit(amountMinor);
        entry.post();
        entries.save(entry);
        lines.saveAll(posting);
        events.publishEvent(new EntryPosted(entryId, from, to, amountMinor, currency, now));
        return entry;
    }

    /** Corrects a posted entry by posting the opposite lines; the original is never edited or deleted. */
    @Transactional
    public JournalEntry reverse(UUID originalId) {
        JournalEntry original = entries.findById(originalId)
                .orElseThrow(() -> new NotFoundException("Entry " + originalId + " not found"));
        if (original.isReversal() || original.getStatus() != EntryStatus.POSTED || entries.existsByReversalOf(originalId)) {
            throw new BusinessRuleViolation("NOT_REVERSIBLE", "Entry " + originalId + " cannot be reversed");
        }
        Instant now = clock.instant();
        UUID reversalId = UUID.randomUUID();
        List<LedgerLine> opposite = lines.findByEntryId(originalId).stream()
                .map(l -> new LedgerLine(reversalId, l.getAccountNumber(), l.getDirection().opposite(), l.getAmountMinor(), now))
                .sorted((a, b) -> a.getAccountNumber().compareTo(b.getAccountNumber()))
                .toList();
        JournalEntry.requireBalanced(opposite);
        for (LedgerLine line : opposite) {
            Account account = accounts.findByNumberForUpdate(line.getAccountNumber()).orElseThrow();
            // A bank-initiated correction may take an account past its overdraft limit.
            account.apply(line.getDirection(), line.getAmountMinor(), false);
        }
        JournalEntry reversal = new JournalEntry(reversalId, "reversal-" + originalId, "Reversal of " + original.getReference(),
                original.getCurrency(), original.getAmountMinor(), originalId, now);
        reversal.post();
        original.markReversed();
        entries.save(reversal);
        lines.saveAll(opposite);
        events.publishEvent(new EntryReversed(originalId, reversalId, now));
        return reversal;
    }

    @Transactional
    public Account changeStatus(String number, AccountStatus next) {
        Account account = accounts.findByNumberForUpdate(number)
                .orElseThrow(() -> new NotFoundException("Account " + number + " not found"));
        AccountStatus previous = account.getStatus();
        account.transitionTo(next);
        events.publishEvent(new AccountStatusChanged(number, previous, next));
        return account;
    }

    @Transactional(readOnly = true)
    public Account account(String number) {
        return accounts.findByNumber(number).orElseThrow(() -> new NotFoundException("Account " + number + " not found"));
    }

    @Transactional(readOnly = true)
    public List<LedgerLine> statement(String number) {
        account(number);
        return lines.findByAccountNumberOrderByPostedAtDesc(number);
    }
}
