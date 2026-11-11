package com.dke.capital_markets.trade_execution_and_smart_order_routing.domain;

import java.util.Set;

/** Lifecycle of a parent order. Pre-trade rejections never create an order; they only publish an event. */
public enum OrderStatus {
    NEW, ROUTED, PARTIALLY_FILLED, FILLED, CANCELLED;

    public boolean canMoveTo(OrderStatus next) {
        return switch (this) {
            case NEW -> Set.of(ROUTED, CANCELLED).contains(next);
            case ROUTED -> Set.of(PARTIALLY_FILLED, FILLED, CANCELLED).contains(next);
            case PARTIALLY_FILLED -> Set.of(PARTIALLY_FILLED, FILLED, CANCELLED).contains(next);
            case FILLED, CANCELLED -> false;
        };
    }

    public boolean isTerminal() {
        return this == FILLED || this == CANCELLED;
    }
}
