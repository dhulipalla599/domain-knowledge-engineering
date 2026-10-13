package com.dke.insurance.quote_and_rating.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;

@Entity
public class Quote {

    @Id
    private UUID id;
    private String driverName;
    private int driverAge;
    private String state;
    @Enumerated(EnumType.STRING)
    private VehicleType vehicleType;
    private int priorClaims;
    private BigDecimal premium;
    @Enumerated(EnumType.STRING)
    private QuoteStatus status;
    private Instant validUntil;
    private Instant boundAt;

    protected Quote() {
    }

    public Quote(String driverName, int driverAge, String state, VehicleType vehicleType, int priorClaims) {
        this.id = UUID.randomUUID();
        this.driverName = driverName;
        this.driverAge = driverAge;
        this.state = state;
        this.vehicleType = vehicleType;
        this.priorClaims = priorClaims;
        this.status = QuoteStatus.DRAFT;
    }

    public void markRated(BigDecimal premium, Instant validUntil) {
        moveTo(QuoteStatus.RATED);
        this.premium = premium;
        this.validUntil = validUntil;
    }

    public void bind(Instant now) {
        moveTo(QuoteStatus.BOUND);
        this.boundAt = now;
    }

    public void expire() {
        moveTo(QuoteStatus.EXPIRED);
    }

    public boolean isExpiredAt(Instant now) {
        return validUntil != null && now.isAfter(validUntil);
    }

    private void moveTo(QuoteStatus next) {
        if (!status.canMoveTo(next)) {
            throw new QuoteRuleException(HttpStatus.CONFLICT,
                    "Quote " + id + " is " + status + "; cannot move to " + next);
        }
        this.status = next;
    }

    public UUID getId() { return id; }
    public String getDriverName() { return driverName; }
    public int getDriverAge() { return driverAge; }
    public String getState() { return state; }
    public VehicleType getVehicleType() { return vehicleType; }
    public int getPriorClaims() { return priorClaims; }
    public BigDecimal getPremium() { return premium; }
    public QuoteStatus getStatus() { return status; }
    public Instant getValidUntil() { return validUntil; }
    public Instant getBoundAt() { return boundAt; }
}
