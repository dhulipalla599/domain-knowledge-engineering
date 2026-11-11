package com.dke.capital_markets.trade_execution_and_smart_order_routing.integration;

import java.math.BigDecimal;

/** Sends one child order to one venue. Production: a FIX 4.4/5.0 session per venue. */
public interface VenueGateway {

    /** Marketable orders are executed immediately; non-marketable orders rest and return filledQuantity 0. */
    VenueAck send(String venueCode, String symbol, String side, long quantity, BigDecimal limitPrice, boolean marketable);

    record VenueAck(String venueExecId, long filledQuantity, BigDecimal fillPrice) {
    }
}
