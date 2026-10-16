package com.dke.retail.product_catalog_and_pricing.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.math.BigDecimal;

@Entity
public class Product {

    @Id
    private String sku;
    private String name;
    private BigDecimal cost;
    private BigDecimal price;
    private String currency;

    protected Product() {
    }

    public Product(String sku, String name, BigDecimal cost, BigDecimal price, String currency) {
        this.sku = sku;
        this.name = name;
        this.cost = cost;
        this.price = price;
        this.currency = currency;
    }

    public void changePrice(BigDecimal newPrice) {
        this.price = newPrice;
    }

    public String getSku() { return sku; }
    public String getName() { return name; }
    public BigDecimal getCost() { return cost; }
    public BigDecimal getPrice() { return price; }
    public String getCurrency() { return currency; }
}
