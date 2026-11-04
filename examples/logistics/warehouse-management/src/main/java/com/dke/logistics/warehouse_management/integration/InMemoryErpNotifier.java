package com.dke.logistics.warehouse_management.integration;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

/** Deterministic stand-in that records calls instead of contacting an ERP. */
@Component
public class InMemoryErpNotifier implements ErpNotifier {

    private final List<String> sent = new CopyOnWriteArrayList<>();

    @Override
    public void inventoryAvailable(String sku, String binCode, int quantity) {
        sent.add("%s x%d @ %s".formatted(sku, quantity, binCode));
    }

    public List<String> sent() {
        return List.copyOf(sent);
    }
}
