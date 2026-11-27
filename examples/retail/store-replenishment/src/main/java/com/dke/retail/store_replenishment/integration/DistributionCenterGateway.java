package com.dke.retail.store_replenishment.integration;

import com.dke.retail.store_replenishment.domain.ReplenishmentOrder;

/** Port to the distribution center's warehouse management system (WMS). */
public interface DistributionCenterGateway {

    /** Submits a released store order for picking and returns the DC's own order reference. */
    String submit(ReplenishmentOrder order);
}
