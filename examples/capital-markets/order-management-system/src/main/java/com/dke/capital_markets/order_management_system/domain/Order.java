package com.dke.capital_markets.order_management_system.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders", uniqueConstraints = @UniqueConstraint(columnNames = {"accountId", "clientOrderId"}))
public class Order {

    @Id
    @GeneratedValue
    private Long id;
    private String accountId;
    private String clientOrderId;
    private String symbol;
    @Enumerated(EnumType.STRING)
    private Side side;
    private long quantity;
    private BigDecimal limitPrice;
    private long filledQuantity;
    private BigDecimal reservedCash = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.NEW;
    private String rejectReason;
    private Instant createdAt = Instant.now();
    @Version
    private long version;

    protected Order() {
    }

    public Order(String accountId, String clientOrderId, String symbol, Side side, long quantity, BigDecimal limitPrice) {
        this.accountId = accountId;
        this.clientOrderId = clientOrderId;
        this.symbol = symbol;
        this.side = side;
        this.quantity = quantity;
        this.limitPrice = limitPrice;
    }

    public BigDecimal notional() {
        return limitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public long remainingQuantity() {
        return quantity - filledQuantity;
    }

    public void accept(BigDecimal cashReserved) {
        moveTo(OrderStatus.ACCEPTED);
        this.reservedCash = cashReserved;
    }

    public void reject(String reason) {
        moveTo(OrderStatus.REJECTED);
        this.rejectReason = reason;
    }

    /** Applies an execution and returns the cash to give back to the account (price improvement). */
    public BigDecimal applyFill(long fillQuantity, BigDecimal fillPrice) {
        if (!status.isOpen()) {
            throw new IllegalStateException("order is " + status + ", cannot take an execution");
        }
        if (fillQuantity <= 0 || fillQuantity > remainingQuantity()) {
            throw new IllegalArgumentException("fill quantity " + fillQuantity + " exceeds remaining " + remainingQuantity());
        }
        filledQuantity += fillQuantity;
        moveTo(remainingQuantity() == 0 ? OrderStatus.FILLED : OrderStatus.PARTIALLY_FILLED);
        if (side == Side.SELL) {
            return BigDecimal.ZERO;
        }
        BigDecimal improvement = limitPrice.subtract(fillPrice).multiply(BigDecimal.valueOf(fillQuantity));
        reservedCash = reservedCash.subtract(limitPrice.multiply(BigDecimal.valueOf(fillQuantity)));
        return improvement;
    }

    /** Cancels the open quantity and returns the cash still reserved for it. */
    public BigDecimal cancel() {
        moveTo(OrderStatus.CANCELED);
        BigDecimal refund = reservedCash;
        reservedCash = BigDecimal.ZERO;
        return refund;
    }

    private void moveTo(OrderStatus target) {
        if (!status.next().contains(target)) {
            throw new IllegalStateException("illegal transition " + status + " -> " + target);
        }
        status = target;
    }

    public Long getId() { return id; }
    public String getAccountId() { return accountId; }
    public String getClientOrderId() { return clientOrderId; }
    public String getSymbol() { return symbol; }
    public Side getSide() { return side; }
    public long getQuantity() { return quantity; }
    public BigDecimal getLimitPrice() { return limitPrice; }
    public long getFilledQuantity() { return filledQuantity; }
    public BigDecimal getReservedCash() { return reservedCash; }
    public OrderStatus getStatus() { return status; }
    public String getRejectReason() { return rejectReason; }
    public Instant getCreatedAt() { return createdAt; }
}
