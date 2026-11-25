package com.dke.insurance.billing_and_premium_collection.api;

import com.dke.insurance.billing_and_premium_collection.domain.Installment;
import com.dke.insurance.billing_and_premium_collection.domain.InstallmentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public record InstallmentResponse(Long id, String policyNumber, int sequenceNo, BigDecimal amountDue,
                                  BigDecimal amountPaid, BigDecimal outstanding, LocalDate dueDate,
                                  LocalDate graceEndsOn, InstallmentStatus status) {

    static InstallmentResponse from(Installment i) {
        return new InstallmentResponse(i.getId(), i.getPolicyNumber(), i.getSequenceNo(), i.getAmountDue(),
                i.getAmountPaid(), i.outstanding(), i.getDueDate(), i.getGraceEndsOn(), i.getStatus());
    }
}
