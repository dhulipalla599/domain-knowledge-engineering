package com.dke.retail.order_management_and_fulfillment.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Domain events. Each record stands in for a message on the Kafka topic named in its TOPIC constant. */
public final class OrderEvents {

    private OrderEvents() {
    }

    public record OrderCreated(String orderNumber, String customerId, BigDecimal totalAmount, String currency,
                               Instant occurredAt) {
        public static final String TOPIC = "order.created";
    }

    public record OrderAllocated(String orderNumber, Map<String, String> nodeBySku, Instant occurredAt) {
        public static final String TOPIC = "order.allocated";
    }

    public record OrderShipped(String orderNumber, List<String> trackingNumbers, Instant occurredAt) {
        public static final String TOPIC = "order.shipped";
    }

    public record OrderDelivered(String orderNumber, Instant occurredAt) {
        public static final String TOPIC = "order.delivered";
    }

    public record OrderCancelled(String orderNumber, String reason, Instant occurredAt) {
        public static final String TOPIC = "order.cancelled";
    }
}
