package com.dke.capital_markets.trade_execution_and_smart_order_routing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;

/** A slice of a parent order sent to one venue. */
@Entity
public class ChildOrder {

    public enum ChildStatus { RESTING, FILLED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "parent_order_id")
    private ParentOrder parent;
    private String venueCode;
    private long quantity;
    @Column(precision = 18, scale = 4)
    private BigDecimal limitPrice;
    private long filledQuantity;
    @Column(precision = 18, scale = 4)
    private BigDecimal fillPrice;
    private String venueExecId;
    @Enumerated(EnumType.STRING)
    private ChildStatus status;

    protected ChildOrder() {
    }

    ChildOrder(ParentOrder parent, String venueCode, long quantity, BigDecimal limitPrice) {
        this.parent = parent;
        this.venueCode = venueCode;
        this.quantity = quantity;
        this.limitPrice = limitPrice;
        this.status = ChildStatus.RESTING;
    }

    void markFilled(long qty, BigDecimal price, String execId) {
        this.filledQuantity = qty;
        this.fillPrice = price;
        this.venueExecId = execId;
        this.status = qty == quantity ? ChildStatus.FILLED : ChildStatus.RESTING;
    }

    void cancelIfResting() {
        if (status == ChildStatus.RESTING) {
            status = ChildStatus.CANCELLED;
        }
    }

    public Long getId() { return id; }
    public String getVenueCode() { return venueCode; }
    public long getQuantity() { return quantity; }
    public BigDecimal getLimitPrice() { return limitPrice; }
    public long getFilledQuantity() { return filledQuantity; }
    public BigDecimal getFillPrice() { return fillPrice; }
    public String getVenueExecId() { return venueExecId; }
    public ChildStatus getStatus() { return status; }
}
