package com.dke.insurance.billing_and_premium_collection.integration;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Deterministic in-memory gateway: token "tok_insufficient_funds" is declined, everything else is approved. */
@Component
public class StubPaymentGateway implements PaymentGateway {

    @Override
    public GatewayResult charge(String paymentToken, BigDecimal amount, String idempotencyKey) {
        if ("tok_insufficient_funds".equals(paymentToken)) {
            return new GatewayResult(false, null, "insufficient_funds");
        }
        var ref = "PSP-" + UUID.nameUUIDFromBytes(idempotencyKey.getBytes()).toString().substring(0, 8);
        return new GatewayResult(true, ref, null);
    }
}
