package com.dke.capital_markets.brokerage_account_opening.integration;

import com.dke.capital_markets.brokerage_account_opening.domain.AccountEvents;
import com.dke.capital_markets.brokerage_account_opening.domain.AccountEvents.AccountActivated;
import com.dke.capital_markets.brokerage_account_opening.domain.AccountEvents.ApplicationDecided;
import com.dke.capital_markets.brokerage_account_opening.domain.AccountEvents.ApplicationSubmitted;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Consumer stand-in that records every event, like an audit sink reading the Kafka topics. */
@Component
public class AuditTrailListener {

    private static final Logger log = LoggerFactory.getLogger(AuditTrailListener.class);
    private final List<String> entries = new CopyOnWriteArrayList<>();

    @EventListener
    void on(ApplicationSubmitted event) {
        record(AccountEvents.SUBMITTED_TOPIC, event.applicationId() + " type=" + event.accountType());
    }

    @EventListener
    void on(ApplicationDecided event) {
        record(AccountEvents.DECIDED_TOPIC, event.applicationId() + " status=" + event.status());
    }

    @EventListener
    void on(AccountActivated event) {
        record(AccountEvents.ACTIVATED_TOPIC, event.applicationId() + " account=" + event.accountNumber());
    }

    public List<String> entries() {
        return List.copyOf(entries);
    }

    private void record(String topic, String detail) {
        String line = topic + " | " + detail;
        entries.add(line);
        log.info("audit {}", line);
    }
}
