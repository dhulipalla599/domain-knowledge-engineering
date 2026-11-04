package com.dke.retail.product_catalog_and_pricing.service;

import com.dke.retail.product_catalog_and_pricing.domain.Product;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/** The core pricing rules. Pure logic, no database, so it is easy to unit test. */
@Component
public class PricingPolicy {

    public static final BigDecimal MIN_MARGIN = new BigDecimal("0.10");
    public static final BigDecimal APPROVAL_THRESHOLD = new BigDecimal("0.20");

    public enum Decision { AUTO_APPLY, NEEDS_APPROVAL }

    /**
     * Rule 1: the price must be positive and keep at least MIN_MARGIN over cost.
     * Rule 2: a move of more than APPROVAL_THRESHOLD (either direction) needs a second pair of eyes.
     */
    public Decision evaluate(Product product, BigDecimal proposed) {
        BigDecimal newPrice = proposed.setScale(2, RoundingMode.HALF_UP);
        if (newPrice.signum() <= 0) {
            throw new PricingException.InvalidPrice("Price must be greater than zero");
        }
        BigDecimal floor = product.getCost().multiply(BigDecimal.ONE.add(MIN_MARGIN)).setScale(2, RoundingMode.HALF_UP);
        if (newPrice.compareTo(floor) < 0) {
            throw new PricingException.BelowMarginFloor(
                    "Price " + newPrice + " is below the margin floor " + floor + " for " + product.getSku());
        }
        BigDecimal movement = newPrice.subtract(product.getPrice()).abs()
                .divide(product.getPrice(), 4, RoundingMode.HALF_UP);
        return movement.compareTo(APPROVAL_THRESHOLD) > 0 ? Decision.NEEDS_APPROVAL : Decision.AUTO_APPLY;
    }
}
