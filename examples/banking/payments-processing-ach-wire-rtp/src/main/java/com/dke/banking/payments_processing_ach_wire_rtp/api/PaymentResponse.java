package com.dke.banking.payments_processing_ach_wire_rtp.api;

import com.dke.banking.payments_processing_ach_wire_rtp.domain.PaymentOrder;
import com.dke.banking.payments_processing_ach_wire_rtp.domain.PaymentStatus;
import com.dke.banking.payments_processing_ach_wire_rtp.domain.Rail;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(UUID id, String fromAccount, Rail rail, String beneficiaryName,
                              BigDecimal amount, PaymentStatus status, String failureReason, Instant createdAt) {

    public static PaymentResponse from(PaymentOrder o) {
        return new PaymentResponse(o.getId(), o.getFromAccount(), o.getRail(), o.getBeneficiaryName(),
                o.getAmount(), o.getStatus(), o.getFailureReason(), o.getCreatedAt());
    }
}
