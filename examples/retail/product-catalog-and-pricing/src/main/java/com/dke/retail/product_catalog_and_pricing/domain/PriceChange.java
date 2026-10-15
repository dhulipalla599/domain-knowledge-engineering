package com.dke.retail.product_catalog_and_pricing.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
public class PriceChange {

    @Id
    @GeneratedValue
    private Long id;
    private String sku;
    private BigDecimal oldPrice;
    private BigDecimal newPrice;
    private String reason;
    @Enumerated(EnumType.STRING)
    private PriceChangeStatus status;
    private Instant requestedAt;
    private Instant decidedAt;

    protected PriceChange() {
    }

    public PriceChange(String sku, BigDecimal oldPrice, BigDecimal newPrice, String reason) {
        this.sku = sku;
        this.oldPrice = oldPrice;
        this.newPrice = newPrice;
        this.reason = reason;
        this.status = PriceChangeStatus.PENDING_APPROVAL;
        this.requestedAt = Instant.now();
    }

    /** Enforces the lifecycle: only a pending change can be applied or rejected. */
    public void moveTo(PriceChangeStatus next) {
        if (!status.allowedNext().contains(next)) {
            throw new IllegalStateException("Price change " + id + " cannot go from " + status + " to " + next);
        }
        this.status = next;
        this.decidedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getSku() { return sku; }
    public BigDecimal getOldPrice() { return oldPrice; }
    public BigDecimal getNewPrice() { return newPrice; }
    public String getReason() { return reason; }
    public PriceChangeStatus getStatus() { return status; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getDecidedAt() { return decidedAt; }
}
