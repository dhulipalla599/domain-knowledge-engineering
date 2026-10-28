package com.dke.capital_markets.order_management_system.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import java.math.BigDecimal;

@Entity
public class Account {

    @Id
    private String id;
    private String owner;
    private BigDecimal buyingPower;
    @Version
    private long version;

    protected Account() {
    }

    public Account(String id, String owner, BigDecimal buyingPower) {
        this.id = id;
        this.owner = owner;
        this.buyingPower = buyingPower;
    }

    public boolean canAfford(BigDecimal amount) {
        return buyingPower.compareTo(amount) >= 0;
    }

    /** Holds cash for an open buy order. */
    public void reserve(BigDecimal amount) {
        if (!canAfford(amount)) {
            throw new IllegalStateException("insufficient buying power");
        }
        buyingPower = buyingPower.subtract(amount);
    }

    public void release(BigDecimal amount) {
        buyingPower = buyingPower.add(amount);
    }

    public String getId() { return id; }
    public String getOwner() { return owner; }
    public BigDecimal getBuyingPower() { return buyingPower; }
}
