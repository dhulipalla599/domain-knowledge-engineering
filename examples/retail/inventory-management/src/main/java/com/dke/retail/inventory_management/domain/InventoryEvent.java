package com.dke.retail.inventory_management.domain;

/** Domain events. topic() is the Kafka topic each one would be published to in production. */
public sealed interface InventoryEvent {

    String topic();

    record InventoryReserved(String reservationId, String orderRef, String sku, int quantity) implements InventoryEvent {
        public String topic() { return "inventory.reserved"; }
    }

    record ReservationConfirmed(String reservationId, String sku, int quantity) implements InventoryEvent {
        public String topic() { return "inventory.confirmed"; }
    }

    record ReservationReleased(String reservationId, String sku, int quantity, ReservationStatus reason) implements InventoryEvent {
        public String topic() { return "inventory.released"; }
    }

    record LowStockDetected(String sku, int available, int reorderPoint) implements InventoryEvent {
        public String topic() { return "inventory.low-stock"; }
    }
}
