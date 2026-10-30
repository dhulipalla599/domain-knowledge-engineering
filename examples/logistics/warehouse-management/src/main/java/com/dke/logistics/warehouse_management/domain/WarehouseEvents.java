package com.dke.logistics.warehouse_management.domain;

/** Domain events; in production each one is a message on the Kafka topic named in its comment. */
public final class WarehouseEvents {

    private WarehouseEvents() {
    }

    /** Kafka topic: warehouse.goods-received */
    public record GoodsReceived(Long receiptId, String asnNumber, String sku, int quantity) {
    }

    /** Kafka topic: warehouse.goods-put-away */
    public record GoodsPutAway(Long receiptId, String sku, String binCode, int quantity) {
    }
}
