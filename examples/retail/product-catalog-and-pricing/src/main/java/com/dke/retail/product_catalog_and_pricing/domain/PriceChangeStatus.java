package com.dke.retail.product_catalog_and_pricing.domain;

import java.util.EnumSet;
import java.util.Set;

/** Lifecycle of a price change. APPLIED and REJECTED are terminal. */
public enum PriceChangeStatus {
    PENDING_APPROVAL,
    APPLIED,
    REJECTED;

    public Set<PriceChangeStatus> allowedNext() {
        return this == PENDING_APPROVAL ? EnumSet.of(APPLIED, REJECTED) : EnumSet.noneOf(PriceChangeStatus.class);
    }
}
