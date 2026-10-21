package com.dke.banking.core_banking_ledger_and_accounts.integration;

import com.dke.banking.core_banking_ledger_and_accounts.domain.LedgerEvents.EntryPosted;
import com.dke.banking.core_banking_ledger_and_accounts.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Stand-in for a notification service consuming ledger.entry.posted to warn customers who are overdrawn. */
@Component
public class LowBalanceAlertListener {

    private static final Logger log = LoggerFactory.getLogger(LowBalanceAlertListener.class);
    private final AccountRepository accounts;

    public LowBalanceAlertListener(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @EventListener
    void on(EntryPosted e) {
        accounts.findByNumber(e.fromAccount())
                .filter(a -> a.getBalanceMinor() < 0)
                .ifPresent(a -> log.warn("ALERT {} is overdrawn: balance {}", a.getNumber(), a.getBalanceMinor()));
    }
}
