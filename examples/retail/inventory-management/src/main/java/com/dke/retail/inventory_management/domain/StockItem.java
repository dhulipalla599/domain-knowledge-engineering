package com.dke.retail.inventory_management.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

/** Stock of one SKU at one location. available = onHand - reserved, and must never go negative. */
@Entity
public class StockItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String sku;
    private String name;
    private int onHand;
    private int reserved;
    private int reorderPoint;

    @Version
    private long version; // optimistic lock: two concurrent reservations cannot both win the last unit

    protected StockItem() {
    }

    public StockItem(String sku, String name, int onHand, int reorderPoint) {
        this.sku = sku;
        this.name = name;
        this.onHand = onHand;
        this.reorderPoint = reorderPoint;
    }

    public int available() {
        return onHand - reserved;
    }

    public boolean belowReorderPoint() {
        return available() <= reorderPoint;
    }

    public void reserve(int quantity) {
        if (quantity > available()) {
            throw new DomainException.InsufficientStock(sku, quantity, available());
        }
        reserved += quantity;
    }

    public void release(int quantity) {
        reserved -= quantity;
    }

    /** Stock physically leaves the building: both on-hand and reserved drop. */
    public void commit(int quantity) {
        onHand -= quantity;
        reserved -= quantity;
    }

    public String getSku() { return sku; }
    public String getName() { return name; }
    public int getOnHand() { return onHand; }
    public int getReserved() { return reserved; }
    public int getReorderPoint() { return reorderPoint; }
}
