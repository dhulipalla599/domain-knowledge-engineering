package com.dke.banking.card_authorization_and_settlement.domain;

/** Issuer-side status of a card; only ACTIVE cards can be authorized. */
public enum CardStatus {
    ACTIVE,
    BLOCKED
}
