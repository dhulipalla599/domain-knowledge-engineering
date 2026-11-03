package com.dke.airline.passenger_name_record_pnr.integration;

import com.dke.airline.passenger_name_record_pnr.domain.Pnr;
import java.util.List;

/** Issues one e-ticket per passenger. Production: an airline ticketing system or a GDS ticketing call. */
public interface TicketingGateway {
    List<String> issueTickets(Pnr pnr);
}
