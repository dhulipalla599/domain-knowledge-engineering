package com.dke.capital_markets.trade_execution_and_smart_order_routing.integration;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/** Fills every marketable child in full at its limit price (the router sized it to displayed liquidity). */
@Component
public class StubVenueGateway implements VenueGateway {

    private final AtomicLong sequence = new AtomicLong(1000);

    @Override
    public VenueAck send(String venueCode, String symbol, String side, long quantity, BigDecimal limitPrice, boolean marketable) {
        String execId = venueCode + "-" + sequence.incrementAndGet();
        return marketable ? new VenueAck(execId, quantity, limitPrice) : new VenueAck(execId, 0, null);
    }
}
