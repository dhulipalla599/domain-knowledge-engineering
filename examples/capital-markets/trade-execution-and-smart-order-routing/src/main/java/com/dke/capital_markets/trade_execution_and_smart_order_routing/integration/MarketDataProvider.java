package com.dke.capital_markets.trade_execution_and_smart_order_routing.integration;

import java.math.BigDecimal;
import java.util.List;

/** Consolidated market data. Production: a direct-feed or SIP market data handler. */
public interface MarketDataProvider {

    /** Top-of-book quote on every venue that currently quotes the symbol. */
    List<VenueQuote> quotes(String symbol);

    boolean isHalted(String symbol);

    record VenueQuote(String venueCode, BigDecimal bidPrice, long bidSize, BigDecimal askPrice, long askSize) {
    }
}
