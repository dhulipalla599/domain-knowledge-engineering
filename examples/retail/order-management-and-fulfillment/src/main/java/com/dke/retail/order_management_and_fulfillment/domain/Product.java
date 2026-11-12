package com.dke.retail.order_management_and_fulfillment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.math.BigDecimal;

/** Catalogue entry: the server-side price and the per-order purchase limit. */
@Entity
public class Product {

    @Id
    private String sku;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;
    @Column(nullable = false)
    private int maxPerOrder;

    protected Product() {
    }

    public Product(String sku, String name, BigDecimal unitPrice, int maxPerOrder) {
        this.sku = sku;
        this.name = name;
        this.unitPrice = unitPrice;
        this.maxPerOrder = maxPerOrder;
    }

    public String getSku() { return sku; }
    public String getName() { return name; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getMaxPerOrder() { return maxPerOrder; }
}
