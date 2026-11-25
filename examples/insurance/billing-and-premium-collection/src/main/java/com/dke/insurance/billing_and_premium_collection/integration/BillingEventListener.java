package com.dke.insurance.billing_and_premium_collection.integration;

import com.dke.insurance.billing_and_premium_collection.domain.BillingEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Stand-in for Kafka consumers (ledger, notifications, policy administration). */
@Component
public class BillingEventListener {

    private static final Logger log = LoggerFactory.getLogger(BillingEventListener.class);
    private final List<String> published = new CopyOnWriteArrayList<>();

    @EventListener
    public void on(BillingEvent event) {
        published.add(event.topic() + ":" + event.policyNumber());
        String action = switch (event) {
            case BillingEvent.PaymentReceived e -> "post to general ledger, send receipt";
            case BillingEvent.PaymentFailed e -> "notify policyholder to update payment method";
            case BillingEvent.InstallmentOverdue e -> "send grace-period reminder";
            case BillingEvent.InstallmentDefaulted e -> "ask policy administration to start lapse";
        };
        log.info("topic={} key={} action='{}' payload={}", event.topic(), event.policyNumber(), action, event);
    }

    public List<String> published() {
        return List.copyOf(published);
    }
}
