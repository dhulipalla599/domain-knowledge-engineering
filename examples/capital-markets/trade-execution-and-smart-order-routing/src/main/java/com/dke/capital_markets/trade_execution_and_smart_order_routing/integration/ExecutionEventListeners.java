package com.dke.capital_markets.trade_execution_and_smart_order_routing.integration;

import com.dke.capital_markets.trade_execution_and_smart_order_routing.domain.ExecutionEvents.FillReported;
import com.dke.capital_markets.trade_execution_and_smart_order_routing.domain.ExecutionEvents.OrderCancelled;
import com.dke.capital_markets.trade_execution_and_smart_order_routing.domain.ExecutionEvents.OrderRejected;
import com.dke.capital_markets.trade_execution_and_smart_order_routing.domain.ExecutionEvents.OrderRouted;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Stand-in for Kafka consumers: post-trade booking, TCA and the compliance audit trail. */
@Component
public class ExecutionEventListeners {

    private static final Logger log = LoggerFactory.getLogger(ExecutionEventListeners.class);
    private final List<String> auditTrail = new CopyOnWriteArrayList<>();

    @EventListener
    public void onRouted(OrderRouted e) {
        record("execution.order-routed", e.clientOrderId() + " -> " + e.childOrders() + " child orders");
    }

    @EventListener
    public void onFill(FillReported e) {
        record("execution.fill-reported", e.venue() + " " + e.quantity() + " @ " + e.price() + " exec " + e.venueExecId());
    }

    @EventListener
    public void onRejected(OrderRejected e) {
        record("execution.order-rejected", e.clientOrderId() + " " + e.rule() + ": " + e.reason());
    }

    @EventListener
    public void onCancelled(OrderCancelled e) {
        record("execution.order-cancelled", e.clientOrderId() + " cancelled " + e.cancelledQuantity());
    }

    private void record(String topic, String text) {
        auditTrail.add(topic + " | " + text);
        log.info("[{}] {}", topic, text);
    }

    public List<String> auditTrail() {
        return List.copyOf(auditTrail);
    }
}
