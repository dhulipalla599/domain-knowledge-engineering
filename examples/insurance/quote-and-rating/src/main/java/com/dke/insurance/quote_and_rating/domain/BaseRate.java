package com.dke.insurance.quote_and_rating.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.math.BigDecimal;

/** Annual base premium per state: stands in for a versioned, filed rate table. */
@Entity
public class BaseRate {

    @Id
    private String state;
    private BigDecimal annualAmount;

    protected BaseRate() {
    }

    public BaseRate(String state, BigDecimal annualAmount) {
        this.state = state;
        this.annualAmount = annualAmount;
    }

    public String getState() { return state; }
    public BigDecimal getAnnualAmount() { return annualAmount; }
}
