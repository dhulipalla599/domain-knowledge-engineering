package com.dke.retail.order_management_and_fulfillment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

/** Stock of one SKU at one node. Available-to-promise = on hand minus already reserved. */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"nodeCode", "sku"}))
public class InventoryPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String nodeCode;
    @Column(nullable = false)
    private String sku;
    @Column(nullable = false)
    private int onHand;
    @Column(nullable = false)
    private int reserved;
    @Version
    private long version;

    protected InventoryPosition() {
    }

    public InventoryPosition(String nodeCode, String sku, int onHand, int reserved) {
        this.nodeCode = nodeCode;
        this.sku = sku;
        this.onHand = onHand;
        this.reserved = reserved;
    }

    public int available() {
        return onHand - reserved;
    }

    public void reserve(int quantity) {
        if (quantity > available()) {
            throw new IllegalStateException("Only %d of %s available at %s".formatted(available(), sku, nodeCode));
        }
        reserved += quantity;
    }

    public void release(int quantity) {
        reserved -= quantity;
    }

    /** Goods left the building: the reservation becomes a real stock decrement. */
    public void consume(int quantity) {
        reserved -= quantity;
        onHand -= quantity;
    }

    public Long getId() { return id; }
    public String getNodeCode() { return nodeCode; }
    public String getSku() { return sku; }
    public int getOnHand() { return onHand; }
    public int getReserved() { return reserved; }
}
