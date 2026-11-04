package com.dke.banking.payments_processing_ach_wire_rtp.integration;

import com.dke.banking.payments_processing_ach_wire_rtp.domain.PaymentEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Stand-in for Kafka consumers (audit, ledger, notifications) subscribed to the payments.* topics. */
@Component
public class PaymentEventListeners {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventListeners.class);
    private final List<String> audit = new CopyOnWriteArrayList<>();

    @EventListener
    public void onAnyPaymentEvent(PaymentEvent event) {
        audit.add(event.topic() + ":" + event.paymentId());
        log.info("[{}] {}", event.topic(), event);
    }

    @EventListener
    public void onSettled(PaymentEvent.PaymentSettled event) {
        log.info("ledger: post {} {} for payment {}", event.rail(), event.amount(), event.paymentId());
    }

    public List<String> audit() {
        return List.copyOf(audit);
    }
}
