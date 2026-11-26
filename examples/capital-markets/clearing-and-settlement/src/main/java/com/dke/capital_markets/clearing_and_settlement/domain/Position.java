package com.dke.capital_markets.clearing_and_settlement.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.math.BigDecimal;

/** A holding at the depository: shares of a symbol, or cash when asset = "USD". */
@Entity
public class Position {

    public static final String CASH = "USD";

    @Id
    private String id;
    private String accountId;
    private String asset;
    private BigDecimal quantity;

    protected Position() {
    }

    public Position(String accountId, String asset, BigDecimal quantity) {
        this.id = key(accountId, asset);
        this.accountId = accountId;
        this.asset = asset;
        this.quantity = quantity;
    }

    public static String key(String accountId, String asset) {
        return accountId + ":" + asset;
    }

    public boolean covers(BigDecimal amount) {
        return quantity.compareTo(amount) >= 0;
    }

    public void debit(BigDecimal amount) {
        if (!covers(amount)) {
            throw new IllegalStateException("position " + id + " cannot cover " + amount);
        }
        quantity = quantity.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        quantity = quantity.add(amount);
    }

    public String getId() { return id; }
    public String getAccountId() { return accountId; }
    public String getAsset() { return asset; }
    public BigDecimal getQuantity() { return quantity; }
}
