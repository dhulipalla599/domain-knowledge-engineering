package com.dke.retail.product_catalog_and_pricing.integration;

import java.math.BigDecimal;

/** Outbound port to the storefront search index. Production adapter: OpenSearch client. */
public interface PriceIndexPort {

    void updatePrice(String sku, BigDecimal price, String currency);

    BigDecimal indexedPrice(String sku);
}
