package com.dke.retail.store_replenishment.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** A request to ship cases of one SKU from the distribution center to one store. */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"store_id", "sku", "run_date"}))
public class ReplenishmentOrder {

    @Id
    private String id;
    private String storeId;
    private String sku;
    private int quantity;
    private String source;     // AUTO (replenishment run) or MANUAL (keyed by the store)
    private LocalDate runDate; // set for AUTO orders: one order per store, SKU and run date
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    private String dcOrderRef;
    private Integer receivedQuantity;
    private Instant createdAt;

    protected ReplenishmentOrder() {
    }

    public ReplenishmentOrder(String storeId, String sku, int quantity, String source, LocalDate runDate) {
        this.id = UUID.randomUUID().toString();
        this.storeId = storeId;
        this.sku = sku;
        this.quantity = quantity;
        this.source = source;
        this.runDate = runDate;
        this.status = OrderStatus.PROPOSED;
        this.createdAt = Instant.now();
    }

    public void release(String dcOrderRef) {
        moveTo(OrderStatus.RELEASED);
        this.dcOrderRef = dcOrderRef;
    }

    public void receive(int received) {
        if (received < 0 || received > quantity) {
            throw new DomainException.InvalidQuantity(
                    "Received quantity must be between 0 and " + quantity + ", was " + received);
        }
        moveTo(OrderStatus.RECEIVED);
        this.receivedQuantity = received;
    }

    public void cancel() {
        moveTo(OrderStatus.CANCELLED);
    }

    private void moveTo(OrderStatus next) {
        if (!status.canMoveTo(next)) {
            throw new DomainException.InvalidTransition(id, status, next);
        }
        status = next;
    }

    public String getId() { return id; }
    public String getStoreId() { return storeId; }
    public String getSku() { return sku; }
    public int getQuantity() { return quantity; }
    public String getSource() { return source; }
    public LocalDate getRunDate() { return runDate; }
    public OrderStatus getStatus() { return status; }
    public String getDcOrderRef() { return dcOrderRef; }
    public Integer getReceivedQuantity() { return receivedQuantity; }
    public Instant getCreatedAt() { return createdAt; }
}
