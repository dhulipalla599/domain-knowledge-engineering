package com.dke.capital_markets.brokerage_account_opening.integration;

import com.dke.capital_markets.brokerage_account_opening.domain.AccountEvents.AccountActivated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Stand-in for a notification service consuming brokerage.account.activated. */
@Component
public class WelcomeNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(WelcomeNotificationListener.class);

    @EventListener
    void on(AccountActivated event) {
        log.info("Sending welcome email for account {}", event.accountNumber());
    }
}
