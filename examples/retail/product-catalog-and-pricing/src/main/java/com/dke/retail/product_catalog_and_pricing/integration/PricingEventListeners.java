package com.dke.retail.product_catalog_and_pricing.integration;

import com.dke.retail.product_catalog_and_pricing.domain.PricingEvents.PriceChangeRejected;
import com.dke.retail.product_catalog_and_pricing.domain.PricingEvents.PriceChangeRequested;
import com.dke.retail.product_catalog_and_pricing.domain.PricingEvents.ProductPriceUpdated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Stand-ins for Kafka consumers. Setting the index is idempotent: replaying an event is harmless. */
@Component
public class PricingEventListeners {

    private static final Logger log = LoggerFactory.getLogger(PricingEventListeners.class);

    private final PriceIndexPort index;

    public PricingEventListeners(PriceIndexPort index) {
        this.index = index;
    }

    @EventListener
    public void onPriceUpdated(ProductPriceUpdated event) {
        index.updatePrice(event.sku(), event.newPrice(), event.currency());
        log.info("retail.product-price.updated sku={} {} -> {}", event.sku(), event.oldPrice(), event.newPrice());
    }

    @EventListener
    public void onApprovalNeeded(PriceChangeRequested event) {
        log.info("retail.price-change.requested id={} sku={} awaiting a pricing manager", event.changeId(), event.sku());
    }

    @EventListener
    public void onRejected(PriceChangeRejected event) {
        log.info("retail.price-change.rejected id={} sku={}", event.changeId(), event.sku());
    }
}
