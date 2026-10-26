package com.dke.data_engineering.real_time_streaming_etl_with_kafka.integration;

import com.dke.data_engineering.real_time_streaming_etl_with_kafka.domain.DomainEvents.OrdersCurated;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Consumer of orders.curated: a tiny real-time serving view (minor units per country). */
@Component
public class RevenueByCountry {

    private final Map<String, Long> totals = new ConcurrentHashMap<>();

    @EventListener
    public void on(OrdersCurated event) {
        totals.merge(event.country(), event.amountCents(), Long::sum);
    }

    public Map<String, Long> snapshot() {
        return new TreeMap<>(totals);
    }
}
