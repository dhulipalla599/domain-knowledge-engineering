package com.dke.healthcare.insurance_eligibility_verification.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** One eligibility inquiry for a coverage, date of service and service type, plus the payer's answer. */
@Entity
public class EligibilityCheck {

    @Id
    private String id;
    private String coverageId;
    private LocalDate serviceDate;
    private String serviceTypeCode;
    @Enumerated(EnumType.STRING)
    private EligibilityStatus status;
    private LocalDate planBeginDate;
    private LocalDate planEndDate;
    private BigDecimal copayAmount;
    private BigDecimal deductibleRemaining;
    private String rejectReasonCode;
    private String message;
    private Instant requestedAt;
    private Instant completedAt;
    @Version
    private long version;

    protected EligibilityCheck() {
    }

    public EligibilityCheck(String id, String coverageId, LocalDate serviceDate, String serviceTypeCode, Instant requestedAt) {
        this.id = id;
        this.coverageId = coverageId;
        this.serviceDate = serviceDate;
        this.serviceTypeCode = serviceTypeCode;
        this.requestedAt = requestedAt;
        this.status = EligibilityStatus.PENDING;
    }

    /** The only way to change status: illegal moves throw and are reported as 409 Conflict. */
    public void transitionTo(EligibilityStatus next) {
        if (!status.canMoveTo(next)) {
            throw new IllegalStateException("Eligibility check " + id + " cannot move from " + status + " to " + next);
        }
        this.status = next;
    }

    public void markActive(LocalDate planBegin, LocalDate planEnd, BigDecimal copay, BigDecimal deductibleLeft, Instant at) {
        transitionTo(EligibilityStatus.ACTIVE);
        record(planBegin, planEnd, at);
        this.copayAmount = copay;
        this.deductibleRemaining = deductibleLeft;
    }

    public void markInactive(LocalDate planBegin, LocalDate planEnd, String reason, Instant at) {
        transitionTo(EligibilityStatus.INACTIVE);
        record(planBegin, planEnd, at);
        this.message = reason;
    }

    public void markRejected(String reasonCode, String reason, Instant at) {
        transitionTo(EligibilityStatus.REJECTED);
        this.rejectReasonCode = reasonCode;
        this.message = reason;
        this.completedAt = at;
    }

    public void markPayerError(String reason, Instant at) {
        transitionTo(EligibilityStatus.PAYER_ERROR);
        this.message = reason;
        this.completedAt = at;
    }

    public void resubmit(Instant at) {
        transitionTo(EligibilityStatus.PENDING);
        this.message = null;
        this.completedAt = null;
        this.requestedAt = at;
    }

    private void record(LocalDate planBegin, LocalDate planEnd, Instant at) {
        this.planBeginDate = planBegin;
        this.planEndDate = planEnd;
        this.completedAt = at;
    }

    public String getId() { return id; }
    public String getCoverageId() { return coverageId; }
    public LocalDate getServiceDate() { return serviceDate; }
    public String getServiceTypeCode() { return serviceTypeCode; }
    public EligibilityStatus getStatus() { return status; }
    public LocalDate getPlanBeginDate() { return planBeginDate; }
    public LocalDate getPlanEndDate() { return planEndDate; }
    public BigDecimal getCopayAmount() { return copayAmount; }
    public BigDecimal getDeductibleRemaining() { return deductibleRemaining; }
    public String getRejectReasonCode() { return rejectReasonCode; }
    public String getMessage() { return message; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getCompletedAt() { return completedAt; }
}
