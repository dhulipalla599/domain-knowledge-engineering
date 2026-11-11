package com.dke.insurance.policy_administration.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** The policy aggregate: terms, current premium, lifecycle status and its endorsement history. */
@Entity
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private String policyNumber;
    @Column(unique = true, nullable = false)
    private String quoteId;
    private String holderName;
    private BigDecimal sumInsured;
    @Column(precision = 7, scale = 4)
    private BigDecimal rate;
    private BigDecimal annualPremium;
    private LocalDate inception;
    private LocalDate expiry;
    @Enumerated(EnumType.STRING)
    private PolicyStatus status;
    private LocalDate cancelledOn;
    private BigDecimal refundDue;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "policy_id")
    private List<Endorsement> endorsements = new ArrayList<>();

    protected Policy() {
    }

    /** A new policy is active from inception for exactly one year. */
    public static Policy issue(String quoteId, String holderName, BigDecimal sumInsured,
                               BigDecimal rate, LocalDate inception) {
        var p = new Policy();
        p.quoteId = quoteId;
        p.holderName = holderName;
        p.sumInsured = sumInsured;
        p.rate = rate;
        p.annualPremium = premiumFor(sumInsured, rate);
        p.inception = inception;
        p.expiry = inception.plusYears(1);
        p.status = PolicyStatus.ACTIVE;
        return p;
    }

    public static BigDecimal premiumFor(BigDecimal sumInsured, BigDecimal rate) {
        return sumInsured.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    public void assignNumber(String number) {
        this.policyNumber = number;
    }

    public void requireActive(String action) {
        if (status != PolicyStatus.ACTIVE) {
            throw new IllegalTransitionException("Cannot " + action + " a policy that is " + status);
        }
    }

    /** Applies a change to the sum insured and records it in the endorsement history. */
    public Endorsement endorse(BigDecimal newSumInsured, BigDecimal newAnnualPremium,
                               LocalDate effectiveDate, BigDecimal premiumDelta, String reason) {
        requireActive("endorse");
        var endorsement = new Endorsement(sumInsured, newSumInsured, effectiveDate, premiumDelta, reason);
        this.sumInsured = newSumInsured;
        this.annualPremium = newAnnualPremium;
        endorsements.add(endorsement);
        return endorsement;
    }

    public void cancel(LocalDate effective, BigDecimal refund) {
        moveTo(PolicyStatus.CANCELLED);
        this.cancelledOn = effective;
        this.refundDue = refund;
    }

    public void expire() {
        moveTo(PolicyStatus.EXPIRED);
    }

    private void moveTo(PolicyStatus target) {
        if (!status.canMoveTo(target)) {
            throw new IllegalTransitionException("Illegal policy transition " + status + " -> " + target);
        }
        this.status = target;
    }

    public Long getId() { return id; }
    public String getPolicyNumber() { return policyNumber; }
    public String getQuoteId() { return quoteId; }
    public String getHolderName() { return holderName; }
    public BigDecimal getSumInsured() { return sumInsured; }
    public BigDecimal getRate() { return rate; }
    public BigDecimal getAnnualPremium() { return annualPremium; }
    public LocalDate getInception() { return inception; }
    public LocalDate getExpiry() { return expiry; }
    public PolicyStatus getStatus() { return status; }
    public LocalDate getCancelledOn() { return cancelledOn; }
    public BigDecimal getRefundDue() { return refundDue; }
    public List<Endorsement> getEndorsements() { return List.copyOf(endorsements); }
}
