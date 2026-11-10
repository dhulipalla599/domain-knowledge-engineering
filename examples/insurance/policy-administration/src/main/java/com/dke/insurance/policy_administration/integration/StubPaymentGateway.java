package com.dke.insurance.policy_administration.integration;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Deterministic stub: approves every charge up to 50,000 and declines anything above. */
@Component
public class StubPaymentGateway implements PaymentGateway {

    private static final BigDecimal LIMIT = new BigDecimal("50000");

    @Override
    public PaymentResult charge(String policyNumber, BigDecimal amount) {
        return amount.compareTo(LIMIT) > 0
                ? new PaymentResult(false, "amount exceeds card limit")
                : new PaymentResult(true, "approved");
    }

    @Override
    public void refund(String policyNumber, BigDecimal amount) {
        // The real adapter would call the PSP; the stub accepts everything.
    }
}
