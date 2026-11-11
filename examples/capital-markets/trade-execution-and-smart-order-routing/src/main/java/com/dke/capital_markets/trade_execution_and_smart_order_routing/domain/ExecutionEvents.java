package com.dke.capital_markets.trade_execution_and_smart_order_routing.domain;

import java.math.BigDecimal;
import java.time.Instant;

/** Domain events. Each record names the Kafka topic it maps to in production. */
public final class ExecutionEvents {

    private ExecutionEvents() {
    }

    /** Topic: execution.order-routed */
    public record OrderRouted(Long orderId, String clientOrderId, String symbol, int childOrders, Instant at) {
    }

    /** Topic: execution.fill-reported (key = orderId, so fills for one order stay ordered) */
    public record FillReported(Long orderId, Long childOrderId, String venue, long quantity,
                               BigDecimal price, String venueExecId, Instant at) {
    }

    /** Topic: execution.order-rejected */
    public record OrderRejected(String clientOrderId, String symbol, String rule, String reason, Instant at) {
    }

    /** Topic: execution.order-cancelled */
    public record OrderCancelled(Long orderId, String clientOrderId, long cancelledQuantity, Instant at) {
    }
}
