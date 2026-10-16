package com.dke.retail.product_catalog_and_pricing.api;

import com.dke.retail.product_catalog_and_pricing.domain.PriceChange;
import com.dke.retail.product_catalog_and_pricing.domain.PriceChangeStatus;
import java.math.BigDecimal;

public record PriceChangeResponse(Long id, String sku, BigDecimal oldPrice, BigDecimal newPrice,
                                  PriceChangeStatus status, String reason) {

    static PriceChangeResponse from(PriceChange c) {
        return new PriceChangeResponse(c.getId(), c.getSku(), c.getOldPrice(), c.getNewPrice(),
                c.getStatus(), c.getReason());
    }
}
