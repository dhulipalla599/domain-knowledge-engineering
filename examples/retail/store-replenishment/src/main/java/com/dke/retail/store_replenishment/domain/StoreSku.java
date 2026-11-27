package com.dke.retail.store_replenishment.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

/**
 * Replenishment parameters and stock for one SKU in one store.
 * inventory position = onHand + onOrder (stock released to the DC but not yet received).
 */
@Entity
public class StoreSku {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String storeId;
    private String sku;
    private int onHand;
    private int onOrder;
    private int reorderPoint;  // order when the position falls to or below this
    private int orderUpTo;     // target position after ordering
    private int casePack;      // the DC ships whole cases only
    private int shelfCapacity; // the most the store can physically hold

    @Version
    private long version; // optimistic lock: a run and a receipt cannot overwrite each other

    protected StoreSku() {
    }

    public StoreSku(String storeId, String sku, int onHand, int onOrder, int reorderPoint,
                    int orderUpTo, int casePack, int shelfCapacity) {
        this.storeId = storeId;
        this.sku = sku;
        this.onHand = onHand;
        this.onOrder = onOrder;
        this.reorderPoint = reorderPoint;
        this.orderUpTo = orderUpTo;
        this.casePack = casePack;
        this.shelfCapacity = shelfCapacity;
    }

    public int inventoryPosition() {
        return onHand + onOrder;
    }

    /**
     * Min/max (s, S) rule: when the position is at or below the reorder point, order up to the target,
     * rounded up to whole cases, then back down one case if that would overflow the shelf.
     */
    public int suggestedQuantity() {
        int position = inventoryPosition();
        if (position > reorderPoint) {
            return 0;
        }
        int needed = orderUpTo - position;
        int quantity = Math.ceilDiv(needed, casePack) * casePack;
        while (quantity > 0 && position + quantity > shelfCapacity) {
            quantity -= casePack;
        }
        return Math.max(quantity, 0);
    }

    /** Rules for a quantity keyed by a person: whole cases only, and it must fit on the shelf. */
    public void checkOrderQuantity(int quantity) {
        if (quantity <= 0) {
            throw new DomainException.InvalidQuantity("Quantity must be positive, was " + quantity);
        }
        if (quantity % casePack != 0) {
            throw new DomainException.CasePackViolation(sku, quantity, casePack);
        }
        if (inventoryPosition() + quantity > shelfCapacity) {
            throw new DomainException.ShelfCapacityExceeded(sku, inventoryPosition(), quantity, shelfCapacity);
        }
    }

    public void addOnOrder(int quantity) {
        onOrder += quantity;
    }

    /** The full ordered quantity leaves on-order; only what physically arrived joins on-hand. */
    public void receive(int ordered, int received) {
        onOrder -= ordered;
        onHand += received;
    }

    public String getStoreId() { return storeId; }
    public String getSku() { return sku; }
    public int getOnHand() { return onHand; }
    public int getOnOrder() { return onOrder; }
    public int getReorderPoint() { return reorderPoint; }
    public int getOrderUpTo() { return orderUpTo; }
    public int getCasePack() { return casePack; }
    public int getShelfCapacity() { return shelfCapacity; }
}
