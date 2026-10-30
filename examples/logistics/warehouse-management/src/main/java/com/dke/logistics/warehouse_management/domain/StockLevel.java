package com.dke.logistics.warehouse_management.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/** On-hand quantity of one SKU in one bin. */
@Entity
public class StockLevel {

    @Id
    @GeneratedValue
    private Long id;
    private String sku;
    private String binCode;
    private int quantity;

    protected StockLevel() {
    }

    public StockLevel(String sku, String binCode, int quantity) {
        this.sku = sku;
        this.binCode = binCode;
        this.quantity = quantity;
    }

    public void add(int qty) {
        this.quantity += qty;
    }

    public String getSku() { return sku; }
    public String getBinCode() { return binCode; }
    public int getQuantity() { return quantity; }
}
