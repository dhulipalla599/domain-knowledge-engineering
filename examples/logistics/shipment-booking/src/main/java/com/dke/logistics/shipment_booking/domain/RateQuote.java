package com.dke.logistics.shipment_booking.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.Instant;

/** A priced offer from a carrier for one lane, valid until {@code validUntil}. */
@Entity
public class RateQuote {

    @Id
    private String id;
    private String carrier;
    private String origin;
    private String destination;
    private String serviceLevel;
    private BigDecimal price;
    private String currency;
    private double maxWeightKg;
    private boolean acceptsDangerousGoods;
    private Instant validUntil;

    protected RateQuote() {
    }

    public RateQuote(String id, String carrier, String origin, String destination, String serviceLevel,
                     BigDecimal price, String currency, double maxWeightKg, boolean acceptsDangerousGoods,
                     Instant validUntil) {
        this.id = id;
        this.carrier = carrier;
        this.origin = origin;
        this.destination = destination;
        this.serviceLevel = serviceLevel;
        this.price = price;
        this.currency = currency;
        this.maxWeightKg = maxWeightKg;
        this.acceptsDangerousGoods = acceptsDangerousGoods;
        this.validUntil = validUntil;
    }

    public boolean isExpiredAt(Instant now) {
        return !now.isBefore(validUntil);
    }

    public String getId() { return id; }
    public String getCarrier() { return carrier; }
    public String getOrigin() { return origin; }
    public String getDestination() { return destination; }
    public String getServiceLevel() { return serviceLevel; }
    public BigDecimal getPrice() { return price; }
    public String getCurrency() { return currency; }
    public double getMaxWeightKg() { return maxWeightKg; }
    public boolean isAcceptsDangerousGoods() { return acceptsDangerousGoods; }
    public Instant getValidUntil() { return validUntil; }
}
