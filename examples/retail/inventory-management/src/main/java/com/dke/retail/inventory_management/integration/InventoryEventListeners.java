package com.dke.retail.inventory_management.integration;

import com.dke.retail.inventory_management.domain.InventoryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** In production these would be Kafka consumers in other services; here they are in-process listeners. */
@Component
public class InventoryEventListeners {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventListeners.class);

    private final ReplenishmentGateway replenishment;

    public InventoryEventListeners(ReplenishmentGateway replenishment) {
        this.replenishment = replenishment;
    }

    @EventListener
    void onLowStock(InventoryEvent.LowStockDetected event) {
        String ref = replenishment.requestReorder(event.sku(), event.available());
        log.info("[{}] {} available={} -> reorder {}", event.topic(), event.sku(), event.available(), ref);
    }

    @EventListener
    void audit(InventoryEvent event) {
        log.info("[{}] {}", event.topic(), event);
    }
}
