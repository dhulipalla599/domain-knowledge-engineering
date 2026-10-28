package com.dke.capital_markets.order_management_system.domain;

import java.math.BigDecimal;

/** Domain events. Each stands in for a Kafka topic named in the type's javadoc. */
public final class OrderEvents {

    private OrderEvents() {
    }

    /** Topic: oms.order.accepted.v1 */
    public record OrderAccepted(Long orderId, String accountId, String symbol, Side side, long quantity, BigDecimal limitPrice) {
    }

    /** Topic: oms.order.execution.v1 */
    public record ExecutionReported(Long orderId, long fillQuantity, BigDecimal fillPrice, OrderStatus statusAfter) {
    }

    /** Topic: oms.order.canceled.v1 */
    public record OrderCanceled(Long orderId, long unfilledQuantity) {
    }
}
