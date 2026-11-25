package com.dke.insurance.billing_and_premium_collection.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** One billed premium installment for a policy: what is due, what has been paid, and when grace ends. */
@Entity
public class Installment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String policyNumber;
    private int sequenceNo;
    @Column(precision = 12, scale = 2)
    private BigDecimal amountDue;
    @Column(precision = 12, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;
    private LocalDate dueDate;
    private LocalDate graceEndsOn;
    @Enumerated(EnumType.STRING)
    private InstallmentStatus status;
    @Version
    private long version;

    protected Installment() {
    }

    public static Installment bill(String policyNumber, int sequenceNo, BigDecimal amountDue,
                                   LocalDate dueDate, int graceDays) {
        var i = new Installment();
        i.policyNumber = policyNumber;
        i.sequenceNo = sequenceNo;
        i.amountDue = amountDue;
        i.dueDate = dueDate;
        i.graceEndsOn = dueDate.plusDays(graceDays);
        i.status = InstallmentStatus.BILLED;
        return i;
    }

    public BigDecimal outstanding() {
        return amountDue.subtract(amountPaid);
    }

    public void requirePayable() {
        if (!InstallmentStatus.OPEN.contains(status)) {
            throw new IllegalTransitionException("Installment " + id + " is " + status + " and cannot take payments");
        }
    }

    /** Allocates a captured payment. Caller has already checked it does not exceed the outstanding amount. */
    public void applyPayment(BigDecimal amount) {
        var paid = amountPaid.add(amount);
        InstallmentStatus target;
        if (paid.compareTo(amountDue) >= 0) {
            target = InstallmentStatus.PAID;
        } else {
            target = status == InstallmentStatus.OVERDUE ? InstallmentStatus.OVERDUE : InstallmentStatus.PARTIALLY_PAID;
        }
        moveTo(target);
        this.amountPaid = paid;
    }

    public boolean isPastDue(LocalDate asOf) {
        return asOf.isAfter(dueDate);
    }

    public boolean isPastGrace(LocalDate asOf) {
        return asOf.isAfter(graceEndsOn);
    }

    public void markOverdue() {
        moveTo(InstallmentStatus.OVERDUE);
    }

    public void markDefaulted() {
        moveTo(InstallmentStatus.DEFAULTED);
    }

    private void moveTo(InstallmentStatus target) {
        if (!status.canMoveTo(target)) {
            throw new IllegalTransitionException("Illegal installment transition " + status + " -> " + target);
        }
        this.status = target;
    }

    public Long getId() { return id; }
    public String getPolicyNumber() { return policyNumber; }
    public int getSequenceNo() { return sequenceNo; }
    public BigDecimal getAmountDue() { return amountDue; }
    public BigDecimal getAmountPaid() { return amountPaid; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDate getGraceEndsOn() { return graceEndsOn; }
    public InstallmentStatus getStatus() { return status; }
}
