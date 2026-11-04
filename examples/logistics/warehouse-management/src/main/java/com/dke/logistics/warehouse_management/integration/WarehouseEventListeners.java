package com.dke.logistics.warehouse_management.integration;

import com.dke.logistics.warehouse_management.domain.WarehouseEvents.GoodsPutAway;
import com.dke.logistics.warehouse_management.domain.WarehouseEvents.GoodsReceived;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Stand-ins for Kafka consumers of warehouse.goods-received and warehouse.goods-put-away. */
@Component
public class WarehouseEventListeners {

    private static final Logger log = LoggerFactory.getLogger(WarehouseEventListeners.class);

    private final ErpNotifier erp;

    public WarehouseEventListeners(ErpNotifier erp) {
        this.erp = erp;
    }

    @EventListener
    void onGoodsReceived(GoodsReceived event) {
        log.info("warehouse.goods-received asn={} sku={} qty={}", event.asnNumber(), event.sku(), event.quantity());
    }

    @EventListener
    void onGoodsPutAway(GoodsPutAway event) {
        erp.inventoryAvailable(event.sku(), event.binCode(), event.quantity());
    }
}
