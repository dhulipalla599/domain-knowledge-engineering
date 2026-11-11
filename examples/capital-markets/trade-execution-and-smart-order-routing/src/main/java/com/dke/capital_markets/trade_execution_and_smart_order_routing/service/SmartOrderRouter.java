package com.dke.capital_markets.trade_execution_and_smart_order_routing.service;

import com.dke.capital_markets.trade_execution_and_smart_order_routing.domain.Side;
import com.dke.capital_markets.trade_execution_and_smart_order_routing.domain.Venue;
import com.dke.capital_markets.trade_execution_and_smart_order_routing.integration.MarketDataProvider.VenueQuote;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Pure routing logic: no I/O, so it is easy to unit test and replay. */
public class SmartOrderRouter {

    public record Slice(String venueCode, long quantity, BigDecimal price, boolean marketable) {
    }

    /**
     * Sweeps displayed liquidity best price first, breaking price ties by the lowest take fee,
     * never crossing the client's limit. Any remainder rests at the limit on the cheapest venue.
     */
    public List<Slice> route(Side side, long quantity, BigDecimal limitPrice,
                             List<VenueQuote> quotes, Map<String, Venue> enabledVenues) {
        Comparator<VenueQuote> byPrice = side == Side.BUY
                ? Comparator.comparing(VenueQuote::askPrice)
                : Comparator.comparing(VenueQuote::bidPrice).reversed();
        Comparator<VenueQuote> byFee = Comparator.comparing(q -> enabledVenues.get(q.venueCode()).getTakeFeePerShare());

        List<Slice> slices = new ArrayList<>();
        long remaining = quantity;
        List<VenueQuote> ranked = quotes.stream()
                .filter(q -> enabledVenues.containsKey(q.venueCode()))
                .sorted(byPrice.thenComparing(byFee))
                .toList();
        for (VenueQuote q : ranked) {
            if (remaining == 0) {
                break;
            }
            BigDecimal price = side == Side.BUY ? q.askPrice() : q.bidPrice();
            long size = side == Side.BUY ? q.askSize() : q.bidSize();
            boolean withinLimit = side == Side.BUY ? price.compareTo(limitPrice) <= 0 : price.compareTo(limitPrice) >= 0;
            if (!withinLimit) {
                break; // ranked by price, so every later venue is worse
            }
            long take = Math.min(remaining, size);
            slices.add(new Slice(q.venueCode(), take, price, true));
            remaining -= take;
        }
        if (remaining > 0) {
            Optional<Venue> cheapest = enabledVenues.values().stream()
                    .min(Comparator.comparing(Venue::getTakeFeePerShare).thenComparing(Venue::getCode));
            long rest = remaining;
            cheapest.ifPresent(v -> slices.add(new Slice(v.getCode(), rest, limitPrice, false)));
        }
        return slices;
    }
}
