package com.dke.retail.inventory_management.integration;

/** Port to the purchasing system. Production adapter: an SAP/ERP purchase-order API or a supplier EDI feed. */
public interface ReplenishmentGateway {
    String requestReorder(String sku, int available);
}
