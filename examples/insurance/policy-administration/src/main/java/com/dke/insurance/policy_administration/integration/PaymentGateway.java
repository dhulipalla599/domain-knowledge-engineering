package com.dke.insurance.policy_administration.integration;

import java.math.BigDecimal;

/** Port to the payment provider. Production adapter: a PSP / billing system client. */
public interface PaymentGateway {

    record PaymentResult(boolean approved, String message) {
    }

    PaymentResult charge(String policyNumber, BigDecimal amount);

    void refund(String policyNumber, BigDecimal amount);
}
