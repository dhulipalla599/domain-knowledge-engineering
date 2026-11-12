package com.dke.retail.order_management_and_fulfillment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;

/** One SKU on an order, priced at order time and sourced from exactly one fulfillment node. */
@Entity
public class OrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private CustomerOrder order;
    @Column(nullable = false)
    private String sku;
    @Column(nullable = false)
    private int quantity;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;
    private String fulfillmentNode;
    private String trackingNumber;

    protected OrderLine() {
    }

    OrderLine(CustomerOrder order, String sku, int quantity, BigDecimal unitPrice) {
        this.order = order;
        this.sku = sku;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public void assignNode(String nodeCode) { this.fulfillmentNode = nodeCode; }
    public void recordShipment(String trackingNumber) { this.trackingNumber = trackingNumber; }

    public Long getId() { return id; }
    public String getSku() { return sku; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public String getFulfillmentNode() { return fulfillmentNode; }
    public String getTrackingNumber() { return trackingNumber; }
}
