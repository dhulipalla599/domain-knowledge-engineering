package com.dke.retail.product_catalog_and_pricing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dke.retail.product_catalog_and_pricing.domain.Product;
import com.dke.retail.product_catalog_and_pricing.service.PricingPolicy.Decision;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PricingPolicyTest {

    private final PricingPolicy policy = new PricingPolicy();
    private final Product shoes = new Product("SKU-1", "Shoes", new BigDecimal("40.00"), new BigDecimal("80.00"), "USD");

    @Test
    void smallChangeAboveFloorAppliesAutomatically() {
        assertThat(policy.evaluate(shoes, new BigDecimal("84.00"))).isEqualTo(Decision.AUTO_APPLY);
    }

    @Test
    void largeChangeNeedsApproval() {
        assertThat(policy.evaluate(shoes, new BigDecimal("60.00"))).isEqualTo(Decision.NEEDS_APPROVAL);
    }

    @Test
    void priceBelowMarginFloorIsRejected() {
        assertThatThrownBy(() -> policy.evaluate(shoes, new BigDecimal("43.99")))
                .isInstanceOf(PricingException.BelowMarginFloor.class);
    }

    @Test
    void priceExactlyAtFloorIsAllowed() {
        assertThat(policy.evaluate(shoes, new BigDecimal("44.00"))).isEqualTo(Decision.NEEDS_APPROVAL);
    }

    @Test
    void nonPositivePriceIsRejected() {
        assertThatThrownBy(() -> policy.evaluate(shoes, BigDecimal.ZERO))
                .isInstanceOf(PricingException.InvalidPrice.class);
    }
}
