package com.dke.retail.order_management_and_fulfillment.integration;

import com.dke.retail.order_management_and_fulfillment.domain.OrderEvents.OrderAllocated;
import com.dke.retail.order_management_and_fulfillment.domain.OrderEvents.OrderCancelled;
import com.dke.retail.order_management_and_fulfillment.domain.OrderEvents.OrderCreated;
import com.dke.retail.order_management_and_fulfillment.domain.OrderEvents.OrderDelivered;
import com.dke.retail.order_management_and_fulfillment.domain.OrderEvents.OrderShipped;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Stand-in for the Kafka consumers (warehouse, notifications, finance). Each method is named after
 * the downstream service and records "topic | payload" so tests can assert what was published.
 */
@Component
public class OrderEventListeners {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListeners.class);
    private final List<String> auditTrail = new CopyOnWriteArrayList<>();

    @EventListener
    public void notifyCustomerOfConfirmation(OrderCreated e) {
        record(OrderCreated.TOPIC, e.orderNumber() + " " + e.totalAmount() + " " + e.currency());
    }

    @EventListener
    public void releaseToWarehouse(OrderAllocated e) {
        record(OrderAllocated.TOPIC, e.orderNumber() + " " + e.nodeBySku());
    }

    @EventListener
    public void sendShippingConfirmation(OrderShipped e) {
        record(OrderShipped.TOPIC, e.orderNumber() + " " + e.trackingNumbers());
    }

    @EventListener
    public void closeOrder(OrderDelivered e) {
        record(OrderDelivered.TOPIC, e.orderNumber());
    }

    @EventListener
    public void refundAndNotify(OrderCancelled e) {
        record(OrderCancelled.TOPIC, e.orderNumber() + " " + e.reason());
    }

    private void record(String topic, String payload) {
        String entry = topic + " | " + payload;
        auditTrail.add(entry);
        log.info(entry);
    }

    public List<String> auditTrail() {
        return List.copyOf(auditTrail);
    }
}
