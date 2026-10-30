package com.dke.logistics.warehouse_management.integration;

/** Port to the ERP / inventory ledger. Production: an HTTP or Kafka adapter. */
public interface ErpNotifier {

    void inventoryAvailable(String sku, String binCode, int quantity);
}
