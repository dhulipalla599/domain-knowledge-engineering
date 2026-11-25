package com.dke.insurance.billing_and_premium_collection.api;

import com.dke.insurance.billing_and_premium_collection.domain.InstallmentStatus;
import com.dke.insurance.billing_and_premium_collection.service.BillingService.PaymentOutcome;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(Long paymentId, Long installmentId, String policyNumber, BigDecimal amount,
                              String gatewayReference, Instant receivedAt, InstallmentStatus installmentStatus,
                              BigDecimal outstanding, boolean replayed) {

    static PaymentResponse from(PaymentOutcome o) {
        var p = o.payment();
        var i = o.installment();
        return new PaymentResponse(p.getId(), i.getId(), i.getPolicyNumber(), p.getAmount(), p.getGatewayReference(),
                p.getReceivedAt(), i.getStatus(), i.outstanding(), o.replayed());
    }
}
