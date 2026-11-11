package com.dke.capital_markets.trade_execution_and_smart_order_routing.integration;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Deterministic, fictional quotes so the example runs offline. */
@Component
public class StubMarketDataProvider implements MarketDataProvider {

    private static final Map<String, List<VenueQuote>> BOOK = Map.of(
            "ACME", List.of(
                    q("XNAS", "100.00", 400, "100.02", 300),
                    q("BATS", "100.00", 300, "100.02", 200),
                    q("ARCX", "99.99", 600, "100.03", 500),
                    q("IEXG", "99.98", 900, "100.05", 1000)),
            "GLBX", List.of(
                    q("XNAS", "41.10", 1000, "41.12", 800),
                    q("ARCX", "41.11", 500, "41.13", 700)),
            "HLTD", List.of(
                    q("XNAS", "12.00", 100, "12.05", 100)));

    private static final Set<String> HALTED = Set.of("HLTD");

    private static VenueQuote q(String venue, String bid, long bidSize, String ask, long askSize) {
        return new VenueQuote(venue, new BigDecimal(bid), bidSize, new BigDecimal(ask), askSize);
    }

    @Override
    public List<VenueQuote> quotes(String symbol) {
        return BOOK.getOrDefault(symbol, List.of());
    }

    @Override
    public boolean isHalted(String symbol) {
        return HALTED.contains(symbol);
    }
}
