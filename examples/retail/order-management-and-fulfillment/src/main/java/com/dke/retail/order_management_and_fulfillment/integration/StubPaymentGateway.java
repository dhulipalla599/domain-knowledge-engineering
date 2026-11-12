package com.dke.retail.order_management_and_fulfillment.integration;

import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Deterministic stand-in for a PSP: the token "tok_declined" is declined, every other token is approved. */
@Component
public class StubPaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(StubPaymentGateway.class);

    @Override
    public Authorization authorize(String paymentToken, BigDecimal amount, String currency, String reference) {
        if ("tok_declined".equals(paymentToken)) {
            return new Authorization(false, null, "insufficient_funds");
        }
        return new Authorization(true, "AUTH-" + reference, null);
    }

    @Override
    public void capture(String authorizationId, BigDecimal amount) {
        log.info("captured {} on {}", amount, authorizationId);
    }

    @Override
    public void voidAuthorization(String authorizationId) {
        log.info("voided {}", authorizationId);
    }
}
