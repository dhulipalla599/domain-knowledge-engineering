package com.dke.insurance.billing_and_premium_collection.integration;

import java.math.BigDecimal;

/** Port to the payment service provider (card or direct debit). */
public interface PaymentGateway {

    GatewayResult charge(String paymentToken, BigDecimal amount, String idempotencyKey);

    record GatewayResult(boolean approved, String reference, String declineReason) {
    }
}
