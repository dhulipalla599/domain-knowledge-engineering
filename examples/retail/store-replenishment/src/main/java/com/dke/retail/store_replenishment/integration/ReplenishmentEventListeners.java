package com.dke.retail.store_replenishment.integration;

import com.dke.retail.store_replenishment.domain.ReplenishmentEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Stand-in for Kafka consumers: store-ops dashboards, DC labour planning and shrink analytics. */
@Component
public class ReplenishmentEventListeners {

    private static final Logger log = LoggerFactory.getLogger(ReplenishmentEventListeners.class);
    private final List<String> published = new CopyOnWriteArrayList<>();

    @EventListener
    public void onEvent(ReplenishmentEvent event) {
        published.add(event.topic());
        log.info("[{}] {}", event.topic(), event);
    }

    @EventListener
    public void onReceived(ReplenishmentEvent.OrderReceived event) {
        if (event.shortQuantity() > 0) {
            log.warn("Short delivery for order {}: {} of {} {} missing",
                    event.orderId(), event.shortQuantity(), event.ordered(), event.sku());
        }
    }

    public List<String> publishedTopics() {
        return List.copyOf(published);
    }
}
