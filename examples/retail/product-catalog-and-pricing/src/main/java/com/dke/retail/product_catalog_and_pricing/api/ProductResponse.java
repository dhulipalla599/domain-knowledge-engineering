package com.dke.retail.product_catalog_and_pricing.api;

import com.dke.retail.product_catalog_and_pricing.domain.Product;
import java.math.BigDecimal;

public record ProductResponse(String sku, String name, BigDecimal cost, BigDecimal price, String currency) {

    static ProductResponse from(Product p) {
        return new ProductResponse(p.getSku(), p.getName(), p.getCost(), p.getPrice(), p.getCurrency());
    }
}
