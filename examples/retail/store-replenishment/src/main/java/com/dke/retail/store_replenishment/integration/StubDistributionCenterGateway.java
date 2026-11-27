package com.dke.retail.store_replenishment.integration;

import com.dke.retail.store_replenishment.domain.ReplenishmentOrder;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/** Deterministic in-memory stand-in for the DC's WMS: hands out sequential order references. */
@Component
public class StubDistributionCenterGateway implements DistributionCenterGateway {

    private final AtomicInteger sequence = new AtomicInteger(1000);

    @Override
    public String submit(ReplenishmentOrder order) {
        return "DC-EAST-" + sequence.incrementAndGet();
    }
}
