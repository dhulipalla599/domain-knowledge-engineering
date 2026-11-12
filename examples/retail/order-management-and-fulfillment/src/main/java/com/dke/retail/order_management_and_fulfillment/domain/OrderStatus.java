package com.dke.retail.order_management_and_fulfillment.domain;

/** Lifecycle of a customer order. Terminal states: DELIVERED and CANCELLED. */
public enum OrderStatus {
    CREATED,
    ALLOCATED,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    public boolean canMoveTo(OrderStatus next) {
        return switch (this) {
            case CREATED -> next == ALLOCATED || next == CANCELLED;
            case ALLOCATED -> next == SHIPPED || next == CANCELLED;
            case SHIPPED -> next == DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }
}
