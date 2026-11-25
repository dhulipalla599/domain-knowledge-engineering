package com.dke.data_engineering.data_quality_and_validation.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated claim in the trusted table. Keyed by claim id, so reloading is an upsert. */
@Entity
@Table(name = "claim")
public class Claim {

    @Id
    private String claimId;

    @Column(nullable = false)
    private String policyNumber;

    @Column(nullable = false)
    private LocalDate lossDate;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal claimAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private Long loadedByRunId;

    protected Claim() { }

    public Claim(String claimId, String policyNumber, LocalDate lossDate, BigDecimal claimAmount,
                 String currency, Long loadedByRunId) {
        this.claimId = claimId;
        this.policyNumber = policyNumber;
        this.lossDate = lossDate;
        this.claimAmount = claimAmount;
        this.currency = currency;
        this.loadedByRunId = loadedByRunId;
    }

    public String getClaimId() { return claimId; }
    public String getPolicyNumber() { return policyNumber; }
    public LocalDate getLossDate() { return lossDate; }
    public BigDecimal getClaimAmount() { return claimAmount; }
    public String getCurrency() { return currency; }
    public Long getLoadedByRunId() { return loadedByRunId; }
}
