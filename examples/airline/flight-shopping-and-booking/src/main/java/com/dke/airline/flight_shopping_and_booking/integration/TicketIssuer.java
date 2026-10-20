package com.dke.airline.flight_shopping_and_booking.integration;

import java.util.UUID;

/** Port to the ticketing system. The real adapter creates the PNR and e-ticket in the airline's PSS. */
public interface TicketIssuer {

    record Ticket(String pnr, String ticketNumber) {
    }

    Ticket issue(UUID bookingId, String passengerName);
}
