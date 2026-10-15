package com.dke.retail.product_catalog_and_pricing.domain;

import java.math.BigDecimal;

/** Domain events; each stands in for a Kafka topic named in its comment. */
public final class PricingEvents {

    private PricingEvents() {
    }

    /** Topic: retail.price-change.requested */
    public record PriceChangeRequested(Long changeId, String sku, BigDecimal oldPrice, BigDecimal newPrice) {
    }

    /** Topic: retail.price-change.rejected */
    public record PriceChangeRejected(Long changeId, String sku) {
    }

    /** Topic: retail.product-price.updated (consumed by search, storefront cache and shelf labels) */
    public record ProductPriceUpdated(String sku, BigDecimal oldPrice, BigDecimal newPrice, String currency) {
    }
}
