package com.dke.airline.flight_shopping_and_booking.integration;

import java.math.BigDecimal;

/** Port to the payment service provider. The real adapter calls PSP authorize/capture with 3-D Secure. */
public interface PaymentGateway {

    boolean authorize(String paymentToken, BigDecimal amount, String currency);
}
