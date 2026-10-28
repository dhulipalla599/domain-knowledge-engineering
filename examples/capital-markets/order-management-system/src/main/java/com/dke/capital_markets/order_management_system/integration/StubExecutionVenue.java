package com.dke.capital_markets.order_management_system.integration;

import com.dke.capital_markets.order_management_system.domain.OrderEvents.OrderAccepted;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

/** Deterministic in-memory venue: remembers what was routed; fills arrive through the REST API. */
@Component
public class StubExecutionVenue implements ExecutionVenue {

    private final List<String> routed = new CopyOnWriteArrayList<>();

    @Override
    public String route(OrderAccepted order) {
        String ref = "VENUE-" + order.orderId();
        routed.add(ref);
        return ref;
    }

    public List<String> routed() {
        return List.copyOf(routed);
    }
}
