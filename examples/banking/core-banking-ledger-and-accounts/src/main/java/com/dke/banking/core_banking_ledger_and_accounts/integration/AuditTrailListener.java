package com.dke.banking.core_banking_ledger_and_accounts.integration;

import com.dke.banking.core_banking_ledger_and_accounts.domain.LedgerEvents.AccountStatusChanged;
import com.dke.banking.core_banking_ledger_and_accounts.domain.LedgerEvents.EntryPosted;
import com.dke.banking.core_banking_ledger_and_accounts.domain.LedgerEvents.EntryReversed;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Stand-in for a Kafka consumer that appends every ledger event to an audit store. */
@Component
public class AuditTrailListener {

    private static final Logger log = LoggerFactory.getLogger(AuditTrailListener.class);
    private final List<String> trail = new CopyOnWriteArrayList<>();

    @EventListener
    void on(EntryPosted e) {
        record("ledger.entry.posted " + e.entryId() + " " + e.fromAccount() + "->" + e.toAccount()
                + " " + e.amountMinor() + " " + e.currency());
    }

    @EventListener
    void on(EntryReversed e) {
        record("ledger.entry.reversed " + e.originalEntryId() + " by " + e.reversalEntryId());
    }

    @EventListener
    void on(AccountStatusChanged e) {
        record("ledger.account.status-changed " + e.accountNumber() + " " + e.from() + "->" + e.to());
    }

    public List<String> trail() {
        return List.copyOf(trail);
    }

    private void record(String line) {
        trail.add(line);
        log.info("AUDIT {}", line);
    }
}
