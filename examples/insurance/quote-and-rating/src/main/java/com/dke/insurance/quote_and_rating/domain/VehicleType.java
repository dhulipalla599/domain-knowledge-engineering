package com.dke.insurance.quote_and_rating.domain;

import java.math.BigDecimal;

/** Vehicle class with its rating factor (illustrative numbers, not a filed rate plan). */
public enum VehicleType {
    SEDAN("1.00"), SUV("1.15"), SPORTS("1.60");

    private final BigDecimal factor;

    VehicleType(String factor) {
        this.factor = new BigDecimal(factor);
    }

    public BigDecimal factor() {
        return factor;
    }
}
