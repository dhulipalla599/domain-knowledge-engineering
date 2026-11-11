package com.dke.capital_markets.trade_execution_and_smart_order_routing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dke.capital_markets.trade_execution_and_smart_order_routing.domain.Side;
import com.dke.capital_markets.trade_execution_and_smart_order_routing.domain.Venue;
import com.dke.capital_markets.trade_execution_and_smart_order_routing.integration.MarketDataProvider.VenueQuote;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SmartOrderRouterTest {

    private final SmartOrderRouter router = new SmartOrderRouter();

    private final Map<String, Venue> venues = Map.of(
            "XNAS", new Venue("XNAS", "Nasdaq", new BigDecimal("0.0030"), true),
            "BATS", new Venue("BATS", "BZX", new BigDecimal("0.0025"), true),
            "ARCX", new Venue("ARCX", "Arca", new BigDecimal("0.0030"), true),
            "IEXG", new Venue("IEXG", "IEX", new BigDecimal("0.0009"), true));

    private final List<VenueQuote> book = List.of(
            new VenueQuote("XNAS", new BigDecimal("100.00"), 400, new BigDecimal("100.02"), 300),
            new VenueQuote("BATS", new BigDecimal("100.00"), 300, new BigDecimal("100.02"), 200),
            new VenueQuote("ARCX", new BigDecimal("99.99"), 600, new BigDecimal("100.03"), 500),
            new VenueQuote("IEXG", new BigDecimal("99.98"), 900, new BigDecimal("100.05"), 1000));

    @Test
    void buySweepsBestPriceFirstAndBreaksTiesOnFee() {
        var slices = router.route(Side.BUY, 800, new BigDecimal("100.04"), book, venues);

        assertThat(slices).containsExactly(
                new SmartOrderRouter.Slice("BATS", 200, new BigDecimal("100.02"), true),
                new SmartOrderRouter.Slice("XNAS", 300, new BigDecimal("100.02"), true),
                new SmartOrderRouter.Slice("ARCX", 300, new BigDecimal("100.03"), true));
    }

    @Test
    void neverCrossesTheLimitAndRestsTheRemainderOnTheCheapestVenue() {
        var slices = router.route(Side.BUY, 700, new BigDecimal("100.02"), book, venues);

        assertThat(slices).containsExactly(
                new SmartOrderRouter.Slice("BATS", 200, new BigDecimal("100.02"), true),
                new SmartOrderRouter.Slice("XNAS", 300, new BigDecimal("100.02"), true),
                new SmartOrderRouter.Slice("IEXG", 200, new BigDecimal("100.02"), false));
    }

    @Test
    void sellHitsHighestBidsFirst() {
        var slices = router.route(Side.SELL, 500, new BigDecimal("100.00"), book, venues);

        assertThat(slices).containsExactly(
                new SmartOrderRouter.Slice("BATS", 300, new BigDecimal("100.00"), true),
                new SmartOrderRouter.Slice("XNAS", 200, new BigDecimal("100.00"), true));
    }

    @Test
    void disabledVenuesAreIgnored() {
        var slices = router.route(Side.BUY, 100, new BigDecimal("100.02"), book, Map.of("XNAS", venues.get("XNAS")));

        assertThat(slices).extracting(SmartOrderRouter.Slice::venueCode).containsExactly("XNAS");
    }

    @Test
    void priceCollarRejectsFatFingerLimits() {
        assertThat(ExecutionService.checkPriceCollar(new BigDecimal("100.04"), book)).isNull();
        assertThat(ExecutionService.checkPriceCollar(new BigDecimal("120.00"), book)).contains("collar 5%");
    }
}
