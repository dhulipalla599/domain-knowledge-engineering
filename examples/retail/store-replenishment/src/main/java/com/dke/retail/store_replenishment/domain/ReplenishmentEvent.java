package com.dke.retail.store_replenishment.domain;

/** Domain events. Each record stands in for one Apache Kafka topic named in topic(). */
public sealed interface ReplenishmentEvent {

    String topic();

    record OrderProposed(String orderId, String storeId, String sku, int quantity, String source)
            implements ReplenishmentEvent {
        public String topic() { return "replenishment.order-proposed"; }
    }

    record OrderReleased(String orderId, String storeId, String sku, int quantity, String dcOrderRef)
            implements ReplenishmentEvent {
        public String topic() { return "replenishment.order-released"; }
    }

    record OrderReceived(String orderId, String storeId, String sku, int ordered, int received)
            implements ReplenishmentEvent {
        public String topic() { return "replenishment.order-received"; }

        public int shortQuantity() { return ordered - received; }
    }
}
