package com.dke.capital_markets.order_management_system.integration;

import com.dke.capital_markets.order_management_system.domain.OrderEvents.ExecutionReported;
import com.dke.capital_markets.order_management_system.domain.OrderEvents.OrderAccepted;
import com.dke.capital_markets.order_management_system.domain.OrderEvents.OrderCanceled;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Stand-in for the Kafka consumers: smart order router and the audit trail writer. */
@Component
public class OrderEventListeners {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListeners.class);

    private final ExecutionVenue venue;
    private final List<String> auditTrail = new CopyOnWriteArrayList<>();

    public OrderEventListeners(ExecutionVenue venue) {
        this.venue = venue;
    }

    @EventListener
    public void onAccepted(OrderAccepted event) {
        String ref = venue.route(event);
        audit("oms.order.accepted.v1 order=" + event.orderId() + " routed=" + ref);
    }

    @EventListener
    public void onExecution(ExecutionReported event) {
        audit("oms.order.execution.v1 order=" + event.orderId() + " qty=" + event.fillQuantity()
                + " px=" + event.fillPrice() + " status=" + event.statusAfter());
    }

    @EventListener
    public void onCanceled(OrderCanceled event) {
        audit("oms.order.canceled.v1 order=" + event.orderId() + " unfilled=" + event.unfilledQuantity());
    }

    public List<String> auditTrail() {
        return List.copyOf(auditTrail);
    }

    private void audit(String line) {
        auditTrail.add(line);
        log.info(line);
    }
}
