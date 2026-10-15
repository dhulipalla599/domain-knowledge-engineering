package com.dke.capital_markets.brokerage_account_opening.domain;

import java.time.Instant;
import java.util.UUID;

/** Domain events. Each record maps to a Kafka topic in production; the topic name is on the constant. */
public final class AccountEvents {

    public static final String SUBMITTED_TOPIC = "brokerage.application.submitted";
    public static final String DECIDED_TOPIC = "brokerage.application.decided";
    public static final String ACTIVATED_TOPIC = "brokerage.account.activated";

    private AccountEvents() {
    }

    public record ApplicationSubmitted(UUID applicationId, AccountType accountType, Instant occurredAt) {
    }

    public record ApplicationDecided(UUID applicationId, ApplicationStatus status, String reason, Instant occurredAt) {
    }

    public record AccountActivated(UUID applicationId, String accountNumber, Instant occurredAt) {
    }
}
