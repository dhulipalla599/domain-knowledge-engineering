package com.dke.capital_markets.order_management_system.integration;

import com.dke.capital_markets.order_management_system.domain.OrderEvents.OrderAccepted;

/** Port to an exchange or broker gateway. Production adapter: FIX 4.4/5.0 session via QuickFIX/J. */
public interface ExecutionVenue {

    /** Sends the order to market and returns the venue's order reference. */
    String route(OrderAccepted order);
}
