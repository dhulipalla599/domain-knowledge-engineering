package com.dke.capital_markets.trade_execution_and_smart_order_routing.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** The client's order. The router splits it into child orders across venues. */
@Entity
public class ParentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String clientOrderId;     // client's idempotency key (FIX tag 11)
    private String accountId;
    private String symbol;
    @Enumerated(EnumType.STRING)
    private Side side;
    private long quantity;
    @Column(precision = 18, scale = 4)
    private BigDecimal limitPrice;
    private long filledQuantity;
    @Column(precision = 18, scale = 4)
    private BigDecimal averagePrice = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.NEW;
    private Instant receivedAt;
    @Version
    private long version;
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id")
    private List<ChildOrder> children = new ArrayList<>();

    protected ParentOrder() {
    }

    public ParentOrder(String clientOrderId, String accountId, String symbol, Side side,
                       long quantity, BigDecimal limitPrice, Instant receivedAt) {
        this.clientOrderId = clientOrderId;
        this.accountId = accountId;
        this.symbol = symbol;
        this.side = side;
        this.quantity = quantity;
        this.limitPrice = limitPrice;
        this.receivedAt = receivedAt;
    }

    public ChildOrder addChild(String venueCode, long qty, BigDecimal price) {
        ChildOrder child = new ChildOrder(this, venueCode, qty, price);
        children.add(child);
        return child;
    }

    public void markRouted() {
        moveTo(OrderStatus.ROUTED);
    }

    /** Applies a venue execution and recomputes the volume-weighted average price. */
    public void applyFill(ChildOrder child, long qty, BigDecimal price, String execId) {
        if (qty <= 0 || filledQuantity + qty > quantity) {
            throw new IllegalStateException("Fill of " + qty + " would overfill order " + clientOrderId);
        }
        BigDecimal notional = averagePrice.multiply(BigDecimal.valueOf(filledQuantity))
                .add(price.multiply(BigDecimal.valueOf(qty)));
        filledQuantity += qty;
        averagePrice = notional.divide(BigDecimal.valueOf(filledQuantity), 4, RoundingMode.HALF_UP);
        child.markFilled(qty, price, execId);
        moveTo(filledQuantity == quantity ? OrderStatus.FILLED : OrderStatus.PARTIALLY_FILLED);
    }

    public void cancel() {
        moveTo(OrderStatus.CANCELLED);
        children.forEach(ChildOrder::cancelIfResting);
    }

    private void moveTo(OrderStatus next) {
        if (!status.canMoveTo(next)) {
            throw new IllegalStateException("Order " + clientOrderId + " cannot move from " + status + " to " + next);
        }
        status = next;
    }

    public long getLeavesQuantity() {
        return status.isTerminal() ? 0 : quantity - filledQuantity;
    }

    public Long getId() { return id; }
    public String getClientOrderId() { return clientOrderId; }
    public String getAccountId() { return accountId; }
    public String getSymbol() { return symbol; }
    public Side getSide() { return side; }
    public long getQuantity() { return quantity; }
    public BigDecimal getLimitPrice() { return limitPrice; }
    public long getFilledQuantity() { return filledQuantity; }
    public BigDecimal getAveragePrice() { return averagePrice; }
    public OrderStatus getStatus() { return status; }
    public Instant getReceivedAt() { return receivedAt; }
    public List<ChildOrder> getChildren() { return children; }
}
