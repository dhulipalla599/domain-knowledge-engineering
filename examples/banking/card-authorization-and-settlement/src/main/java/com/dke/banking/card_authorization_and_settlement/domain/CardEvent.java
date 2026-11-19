package com.dke.banking.card_authorization_and_settlement.domain;

import java.math.BigDecimal;
import java.util.UUID;

/** Domain events; topic() is the Apache Kafka topic each one would be published to. */
public sealed interface CardEvent {

    UUID authorizationId();

    String topic();

    record AuthorizationApproved(UUID authorizationId, String cardToken, BigDecimal amount, String mcc)
            implements CardEvent {
        public String topic() { return "card.authorization.approved"; }
    }

    record AuthorizationDeclined(UUID authorizationId, String cardToken, String responseCode)
            implements CardEvent {
        public String topic() { return "card.authorization.declined"; }
    }

    record AuthorizationReversed(UUID authorizationId, String cardToken, BigDecimal amount)
            implements CardEvent {
        public String topic() { return "card.authorization.reversed"; }
    }

    record TransactionCleared(UUID authorizationId, String cardToken, BigDecimal authorizedAmount,
                              BigDecimal clearedAmount) implements CardEvent {
        public String topic() { return "card.transaction.cleared"; }
    }
}
