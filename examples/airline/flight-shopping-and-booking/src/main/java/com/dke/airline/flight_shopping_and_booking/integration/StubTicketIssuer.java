package com.dke.airline.flight_shopping_and_booking.integration;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class StubTicketIssuer implements TicketIssuer {

    private final AtomicLong sequence = new AtomicLong(1000);

    @Override
    public Ticket issue(UUID bookingId, String passengerName) {
        long n = sequence.incrementAndGet();
        return new Ticket("DK" + n, "0012" + String.format("%09d", n));
    }
}
