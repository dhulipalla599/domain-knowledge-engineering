package com.dke.healthcare.insurance_eligibility_verification.api;

import com.dke.healthcare.insurance_eligibility_verification.domain.EligibilityCheck;
import com.dke.healthcare.insurance_eligibility_verification.domain.EligibilityStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record EligibilityCheckResponse(String id, String coverageId, LocalDate serviceDate, String serviceTypeCode,
                                       EligibilityStatus status, LocalDate planBeginDate, LocalDate planEndDate,
                                       BigDecimal copayAmount, BigDecimal deductibleRemaining,
                                       String rejectReasonCode, String message, Instant completedAt, boolean reused) {

    static EligibilityCheckResponse of(EligibilityCheck c, boolean reused) {
        return new EligibilityCheckResponse(c.getId(), c.getCoverageId(), c.getServiceDate(), c.getServiceTypeCode(),
                c.getStatus(), c.getPlanBeginDate(), c.getPlanEndDate(), c.getCopayAmount(), c.getDeductibleRemaining(),
                c.getRejectReasonCode(), c.getMessage(), c.getCompletedAt(), reused);
    }
}
