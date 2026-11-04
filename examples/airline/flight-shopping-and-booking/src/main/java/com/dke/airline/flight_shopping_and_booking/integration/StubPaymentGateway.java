package com.dke.airline.flight_shopping_and_booking.integration;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Deterministic stand-in: only the token "tok_declined" is refused. */
@Component
public class StubPaymentGateway implements PaymentGateway {

    @Override
    public boolean authorize(String paymentToken, BigDecimal amount, String currency) {
        return !"tok_declined".equals(paymentToken);
    }
}
