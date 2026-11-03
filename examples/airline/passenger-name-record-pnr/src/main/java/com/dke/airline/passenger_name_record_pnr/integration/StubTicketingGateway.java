package com.dke.airline.passenger_name_record_pnr.integration;

import com.dke.airline.passenger_name_record_pnr.domain.Pnr;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.stereotype.Component;

/** Deterministic stub: ticket numbers are derived from the record locator. */
@Component
public class StubTicketingGateway implements TicketingGateway {

    @Override
    public List<String> issueTickets(Pnr pnr) {
        return IntStream.rangeClosed(1, pnr.getPassengers().size())
                .mapToObj(i -> "176-" + pnr.getRecordLocator() + "-" + i)
                .toList();
    }
}
