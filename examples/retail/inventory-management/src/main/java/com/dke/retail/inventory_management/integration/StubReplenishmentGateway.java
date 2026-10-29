package com.dke.retail.inventory_management.integration;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

/** Deterministic in-memory stand-in: records the requests it receives. */
@Component
public class StubReplenishmentGateway implements ReplenishmentGateway {

    private final List<String> requests = new CopyOnWriteArrayList<>();

    @Override
    public String requestReorder(String sku, int available) {
        String ref = "PO-" + sku + "-" + (requests.size() + 1);
        requests.add(ref);
        return ref;
    }

    public List<String> requests() {
        return List.copyOf(requests);
    }
}
