package com.dke.insurance.policy_administration.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** A mid-term change to a policy. The premium delta is already pro-rated for the remaining term. */
@Entity
public class Endorsement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private BigDecimal previousSumInsured;
    private BigDecimal newSumInsured;
    private LocalDate effectiveDate;
    private BigDecimal premiumDelta;
    private String reason;

    protected Endorsement() {
    }

    Endorsement(BigDecimal previousSumInsured, BigDecimal newSumInsured, LocalDate effectiveDate,
                BigDecimal premiumDelta, String reason) {
        this.previousSumInsured = previousSumInsured;
        this.newSumInsured = newSumInsured;
        this.effectiveDate = effectiveDate;
        this.premiumDelta = premiumDelta;
        this.reason = reason;
    }

    public Long getId() { return id; }
    public BigDecimal getPreviousSumInsured() { return previousSumInsured; }
    public BigDecimal getNewSumInsured() { return newSumInsured; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public BigDecimal getPremiumDelta() { return premiumDelta; }
    public String getReason() { return reason; }
}
