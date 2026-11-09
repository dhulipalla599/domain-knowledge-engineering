package com.dke.data_engineering.batch_etl_and_workflow_orchestration.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Warehouse row. order_id is the natural key, so reloading a file overwrites instead of duplicating. */
@Entity
@Table(name = "sales_fact", uniqueConstraints = @UniqueConstraint(columnNames = "orderId"))
public class SalesFact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String orderId;
    private String customerId;
    private String sku;
    private int quantity;
    @Column(precision = 12, scale = 2)
    private BigDecimal unitPrice;
    @Column(precision = 14, scale = 2)
    private BigDecimal totalAmount;
    private String currency;
    private LocalDate businessDate;

    protected SalesFact() { }

    public SalesFact(String orderId, String customerId, String sku, int quantity,
                     BigDecimal unitPrice, String currency, LocalDate businessDate) {
        this.orderId = orderId;
        overwrite(customerId, sku, quantity, unitPrice, currency, businessDate);
    }

    public final void overwrite(String customerId, String sku, int quantity,
                                BigDecimal unitPrice, String currency, LocalDate businessDate) {
        this.customerId = customerId;
        this.sku = sku;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        this.currency = currency;
        this.businessDate = businessDate;
    }

    public Long getId() { return id; }
    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public String getSku() { return sku; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getCurrency() { return currency; }
    public LocalDate getBusinessDate() { return businessDate; }
}
