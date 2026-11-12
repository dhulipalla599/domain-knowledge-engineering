package com.dke.retail.order_management_and_fulfillment.domain;

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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** The customer's order: what was bought, how it was paid and where it is in its lifecycle. */
@Entity
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private String orderNumber;
    @Column(nullable = false, unique = true)
    private String idempotencyKey;
    @Column(nullable = false)
    private String customerId;
    @Column(nullable = false)
    private String shipToRegion;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;
    @Column(nullable = false, length = 3)
    private String currency;
    private String paymentAuthorizationId;
    @Column(nullable = false)
    private Instant placedAt;
    @Version
    private long version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id")
    private List<OrderLine> lines = new ArrayList<>();

    protected CustomerOrder() {
    }

    public static CustomerOrder create(String customerId, String idempotencyKey, String shipToRegion,
                                       String currency, Instant placedAt) {
        CustomerOrder o = new CustomerOrder();
        o.customerId = customerId;
        o.idempotencyKey = idempotencyKey;
        o.shipToRegion = shipToRegion;
        o.currency = currency;
        o.placedAt = placedAt;
        o.status = OrderStatus.CREATED;
        return o;
    }

    public OrderLine addLine(String sku, int quantity, BigDecimal unitPrice) {
        OrderLine line = new OrderLine(this, sku, quantity, unitPrice);
        lines.add(line);
        totalAmount = totalAmount.add(line.lineTotal());
        return line;
    }

    public void assignOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public void recordAuthorization(String authorizationId) {
        this.paymentAuthorizationId = authorizationId;
    }

    public void markAllocated() { moveTo(OrderStatus.ALLOCATED); }
    public void markShipped() { moveTo(OrderStatus.SHIPPED); }
    public void markDelivered() { moveTo(OrderStatus.DELIVERED); }
    public void cancel() { moveTo(OrderStatus.CANCELLED); }

    private void moveTo(OrderStatus next) {
        if (!status.canMoveTo(next)) {
            throw new IllegalStateException("Order %s cannot move from %s to %s".formatted(orderNumber, status, next));
        }
        status = next;
    }

    public Long getId() { return id; }
    public String getOrderNumber() { return orderNumber; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getCustomerId() { return customerId; }
    public String getShipToRegion() { return shipToRegion; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getCurrency() { return currency; }
    public String getPaymentAuthorizationId() { return paymentAuthorizationId; }
    public Instant getPlacedAt() { return placedAt; }
    public List<OrderLine> getLines() { return lines; }
}
