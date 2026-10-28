package com.dke.capital_markets.order_management_system.service;

import com.dke.capital_markets.order_management_system.domain.Account;
import com.dke.capital_markets.order_management_system.domain.Order;
import com.dke.capital_markets.order_management_system.domain.OrderEvents.ExecutionReported;
import com.dke.capital_markets.order_management_system.domain.OrderEvents.OrderAccepted;
import com.dke.capital_markets.order_management_system.domain.OrderEvents.OrderCanceled;
import com.dke.capital_markets.order_management_system.domain.Side;
import com.dke.capital_markets.order_management_system.repository.AccountRepository;
import com.dke.capital_markets.order_management_system.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final BigDecimal TICK = new BigDecimal("0.01");
    private static final long MAX_QUANTITY = 100_000;

    private final OrderRepository orders;
    private final AccountRepository accounts;
    private final ApplicationEventPublisher events;

    public OrderService(OrderRepository orders, AccountRepository accounts, ApplicationEventPublisher events) {
        this.orders = orders;
        this.accounts = accounts;
        this.events = events;
    }

    /** Result of a placement: replays of the same clientOrderId return the original order with created=false. */
    public record Placement(Order order, boolean created) {
    }

    /** Pre-trade checks, then accept. Rules: idempotency, tick size, size limit, buying power. */
    @Transactional
    public Placement place(String accountId, String clientOrderId, String symbol, Side side, long quantity, BigDecimal limitPrice) {
        var replay = orders.findByAccountIdAndClientOrderId(accountId, clientOrderId);
        if (replay.isPresent()) {
            return new Placement(replay.get(), false);
        }
        Account account = accounts.findById(accountId)
                .orElseThrow(() -> new NotFoundException("account " + accountId + " not found"));
        if (quantity <= 0 || quantity > MAX_QUANTITY) {
            throw new RuleViolationException("MAX_ORDER_SIZE", "quantity must be between 1 and " + MAX_QUANTITY);
        }
        if (limitPrice.signum() <= 0 || limitPrice.remainder(TICK).signum() != 0) {
            throw new RuleViolationException("TICK_SIZE", "limit price must be positive and a multiple of " + TICK);
        }
        Order order = new Order(accountId, clientOrderId, symbol, side, quantity, limitPrice);
        if (side == Side.BUY) {
            if (!account.canAfford(order.notional())) {
                throw new RuleViolationException("BUYING_POWER",
                        "order needs " + order.notional() + " but buying power is " + account.getBuyingPower());
            }
            account.reserve(order.notional());
            order.accept(order.notional());
        } else {
            order.accept(BigDecimal.ZERO);
        }
        Order saved = orders.save(order);
        events.publishEvent(new OrderAccepted(saved.getId(), accountId, symbol, side, quantity, limitPrice));
        return new Placement(saved, true);
    }

    /** Applies an execution report from the venue. */
    @Transactional
    public Order reportExecution(Long orderId, long fillQuantity, BigDecimal fillPrice) {
        Order order = get(orderId);
        if (order.getSide() == Side.BUY && fillPrice.compareTo(order.getLimitPrice()) > 0) {
            throw new RuleViolationException("LIMIT_PRICE", "fill price " + fillPrice + " breaches limit " + order.getLimitPrice());
        }
        BigDecimal improvement = order.applyFill(fillQuantity, fillPrice);
        accounts.findById(order.getAccountId()).orElseThrow().release(improvement);
        events.publishEvent(new ExecutionReported(orderId, fillQuantity, fillPrice, order.getStatus()));
        return order;
    }

    @Transactional
    public Order cancel(Long orderId) {
        Order order = get(orderId);
        long unfilled = order.remainingQuantity();
        BigDecimal refund = order.cancel();
        accounts.findById(order.getAccountId()).orElseThrow().release(refund);
        events.publishEvent(new OrderCanceled(orderId, unfilled));
        return order;
    }

    @Transactional(readOnly = true)
    public Order get(Long orderId) {
        return orders.findById(orderId).orElseThrow(() -> new NotFoundException("order " + orderId + " not found"));
    }

    @Transactional(readOnly = true)
    public List<Order> forAccount(String accountId) {
        return orders.findByAccountIdOrderByIdDesc(accountId);
    }
}
