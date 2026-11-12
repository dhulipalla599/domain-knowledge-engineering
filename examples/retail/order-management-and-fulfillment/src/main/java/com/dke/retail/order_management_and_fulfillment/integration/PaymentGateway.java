package com.dke.retail.order_management_and_fulfillment.integration;

import java.math.BigDecimal;

/** Port to the payment service provider (card authorisation, capture, void). */
public interface PaymentGateway {

    record Authorization(boolean approved, String authorizationId, String declineReason) {
    }

    Authorization authorize(String paymentToken, BigDecimal amount, String currency, String reference);

    void capture(String authorizationId, BigDecimal amount);

    void voidAuthorization(String authorizationId);
}
