package com.dke.capital_markets.trade_execution_and_smart_order_routing.service;

import com.dke.capital_markets.trade_execution_and_smart_order_routing.domain.ChildOrder;
import com.dke.capital_markets.trade_execution_and_smart_order_routing.domain.ExecutionEvents.FillReported;
import com.dke.capital_markets.trade_execution_and_smart_order_routing.domain.ExecutionEvents.OrderCancelled;
import com.dke.capital_markets.trade_execution_and_smart_order_routing.domain.ExecutionEvents.OrderRejected;
import com.dke.capital_markets.trade_execution_and_smart_order_routing.domain.ExecutionEvents.OrderRouted;
import com.dke.capital_markets.trade_execution_and_smart_order_routing.domain.ParentOrder;
import com.dke.capital_markets.trade_execution_and_smart_order_routing.domain.Side;
import com.dke.capital_markets.trade_execution_and_smart_order_routing.domain.Venue;
import com.dke.capital_markets.trade_execution_and_smart_order_routing.integration.MarketDataProvider;
import com.dke.capital_markets.trade_execution_and_smart_order_routing.integration.MarketDataProvider.VenueQuote;
import com.dke.capital_markets.trade_execution_and_smart_order_routing.integration.VenueGateway;
import com.dke.capital_markets.trade_execution_and_smart_order_routing.integration.VenueGateway.VenueAck;
import com.dke.capital_markets.trade_execution_and_smart_order_routing.repository.ParentOrderRepository;
import com.dke.capital_markets.trade_execution_and_smart_order_routing.repository.VenueRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExecutionService {

    /** Reject limit prices more than 5% away from the NBBO midpoint (fat-finger protection). */
    static final BigDecimal PRICE_COLLAR = new BigDecimal("0.05");

    private final ParentOrderRepository orders;
    private final VenueRepository venues;
    private final MarketDataProvider marketData;
    private final VenueGateway gateway;
    private final ApplicationEventPublisher events;
    private final SmartOrderRouter router = new SmartOrderRouter();

    public ExecutionService(ParentOrderRepository orders, VenueRepository venues, MarketDataProvider marketData,
                            VenueGateway gateway, ApplicationEventPublisher events) {
        this.orders = orders;
        this.venues = venues;
        this.marketData = marketData;
        this.gateway = gateway;
        this.events = events;
    }

    @Transactional
    public ParentOrder submit(String clientOrderId, String accountId, String symbol, Side side,
                              long quantity, BigDecimal limitPrice) {
        if (orders.findByClientOrderId(clientOrderId).isPresent()) {
            throw reject(clientOrderId, symbol, "DUPLICATE_CLIENT_ORDER_ID",
                    "clientOrderId " + clientOrderId + " was already used", true);
        }
        if (marketData.isHalted(symbol)) {
            throw reject(clientOrderId, symbol, "SYMBOL_HALTED", symbol + " is halted; orders cannot be routed", false);
        }
        List<VenueQuote> quotes = marketData.quotes(symbol);
        if (quotes.isEmpty()) {
            throw reject(clientOrderId, symbol, "NO_MARKET_DATA", "No venue is quoting " + symbol, false);
        }
        String collarBreach = checkPriceCollar(limitPrice, quotes);
        if (collarBreach != null) {
            throw reject(clientOrderId, symbol, "PRICE_COLLAR", collarBreach, false);
        }

        ParentOrder order = new ParentOrder(clientOrderId, accountId, symbol, side, quantity, limitPrice, Instant.now());
        Map<String, Venue> enabled = venues.findByEnabledTrue().stream()
                .collect(Collectors.toMap(Venue::getCode, Function.identity()));
        List<SmartOrderRouter.Slice> plan = router.route(side, quantity, limitPrice, quotes, enabled);

        order.markRouted();
        orders.save(order);
        for (SmartOrderRouter.Slice slice : plan) {
            ChildOrder child = order.addChild(slice.venueCode(), slice.quantity(), slice.price());
            VenueAck ack = gateway.send(slice.venueCode(), symbol, side.name(), slice.quantity(), slice.price(), slice.marketable());
            if (ack.filledQuantity() > 0) {
                order.applyFill(child, ack.filledQuantity(), ack.fillPrice(), ack.venueExecId());
            }
        }
        orders.saveAndFlush(order);

        events.publishEvent(new OrderRouted(order.getId(), clientOrderId, symbol, plan.size(), Instant.now()));
        order.getChildren().stream().filter(c -> c.getFilledQuantity() > 0).forEach(c ->
                events.publishEvent(new FillReported(order.getId(), c.getId(), c.getVenueCode(),
                        c.getFilledQuantity(), c.getFillPrice(), c.getVenueExecId(), Instant.now())));
        return order;
    }

    /** Returns a reason if the limit price is outside the collar around the NBBO midpoint, otherwise null. */
    static String checkPriceCollar(BigDecimal limitPrice, List<VenueQuote> quotes) {
        BigDecimal bestBid = quotes.stream().map(VenueQuote::bidPrice).max(Comparator.naturalOrder()).orElseThrow();
        BigDecimal bestAsk = quotes.stream().map(VenueQuote::askPrice).min(Comparator.naturalOrder()).orElseThrow();
        BigDecimal mid = bestBid.add(bestAsk).divide(BigDecimal.TWO, 4, RoundingMode.HALF_UP);
        BigDecimal deviation = limitPrice.subtract(mid).abs().divide(mid, 4, RoundingMode.HALF_UP);
        if (deviation.compareTo(PRICE_COLLAR) > 0) {
            return "Limit " + limitPrice + " is " + deviation.movePointRight(2).stripTrailingZeros().toPlainString()
                    + "% from NBBO midpoint " + mid + " (collar 5%)";
        }
        return null;
    }

    @Transactional
    public ParentOrder cancel(Long id) {
        ParentOrder order = get(id);
        long leaves = order.getLeavesQuantity();
        order.cancel();
        events.publishEvent(new OrderCancelled(order.getId(), order.getClientOrderId(), leaves, Instant.now()));
        return order;
    }

    @Transactional(readOnly = true)
    public ParentOrder get(Long id) {
        return orders.findById(id).orElseThrow(() -> new NoSuchElementException("Order " + id + " not found"));
    }

    public List<VenueQuote> book(String symbol) {
        return marketData.quotes(symbol);
    }

    private BusinessRuleException reject(String clientOrderId, String symbol, String rule, String reason, boolean conflict) {
        events.publishEvent(new OrderRejected(clientOrderId, symbol, rule, reason, Instant.now()));
        return new BusinessRuleException(rule, reason, conflict);
    }
}
