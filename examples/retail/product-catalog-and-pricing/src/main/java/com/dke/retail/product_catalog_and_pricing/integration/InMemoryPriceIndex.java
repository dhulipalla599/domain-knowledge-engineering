package com.dke.retail.product_catalog_and_pricing.integration;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryPriceIndex implements PriceIndexPort {

    private final Map<String, BigDecimal> prices = new ConcurrentHashMap<>();

    @Override
    public void updatePrice(String sku, BigDecimal price, String currency) {
        prices.put(sku, price);
    }

    @Override
    public BigDecimal indexedPrice(String sku) {
        return prices.get(sku);
    }
}
