package com.dke.banking.card_authorization_and_settlement.domain;

import java.util.Set;

/** Lifecycle of a card authorization (the "hold") from approval to clearing. */
public enum AuthorizationStatus {
    APPROVED,
    DECLINED,
    REVERSED,
    CLEARED;

    /** Only an open hold (APPROVED) can move on; every other state is final. */
    public Set<AuthorizationStatus> allowedNext() {
        return this == APPROVED ? Set.of(REVERSED, CLEARED) : Set.of();
    }

    public boolean canMoveTo(AuthorizationStatus next) {
        return allowedNext().contains(next);
    }
}
