package com.dke.banking.card_authorization_and_settlement.integration;

import com.dke.banking.card_authorization_and_settlement.domain.CardEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Stand-in for Kafka consumers (notifications, statements, audit) subscribed to the card.* topics. */
@Component
public class CardEventListeners {

    private static final Logger log = LoggerFactory.getLogger(CardEventListeners.class);
    private final List<String> audit = new CopyOnWriteArrayList<>();

    @EventListener
    public void onAnyCardEvent(CardEvent event) {
        audit.add(event.topic() + ":" + event.authorizationId());
        log.info("[{}] {}", event.topic(), event);
    }

    @EventListener
    public void onApproved(CardEvent.AuthorizationApproved event) {
        log.info("notify cardholder of {}: purchase of {} approved", event.cardToken(), event.amount());
    }

    @EventListener
    public void onCleared(CardEvent.TransactionCleared event) {
        log.info("statement: post {} to {} (authorized {})",
                event.clearedAmount(), event.cardToken(), event.authorizedAmount());
    }

    public List<String> audit() {
        return List.copyOf(audit);
    }
}
