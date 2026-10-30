package com.dke.logistics.warehouse_management.domain;

/** Lifecycle of an inbound receipt: EXPECTED -> RECEIVED -> PUT_AWAY. */
public enum ReceiptStatus {
    EXPECTED, RECEIVED, PUT_AWAY;

    public boolean canMoveTo(ReceiptStatus next) {
        return switch (this) {
            case EXPECTED -> next == RECEIVED;
            case RECEIVED -> next == PUT_AWAY;
            case PUT_AWAY -> false;
        };
    }
}
