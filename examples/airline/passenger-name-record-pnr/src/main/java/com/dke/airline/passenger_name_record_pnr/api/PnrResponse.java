package com.dke.airline.passenger_name_record_pnr.api;

import com.dke.airline.passenger_name_record_pnr.domain.Pnr;
import com.dke.airline.passenger_name_record_pnr.domain.PnrStatus;
import java.time.Instant;
import java.util.List;

public record PnrResponse(String recordLocator, PnrStatus status, String contactEmail, Instant ticketTimeLimit,
                          List<PassengerDto> passengers, List<SegmentDto> segments, List<String> ticketNumbers) {

    static PnrResponse from(Pnr p) {
        return new PnrResponse(p.getRecordLocator(), p.getStatus(), p.getContactEmail(), p.getTicketTimeLimit(),
                p.getPassengers().stream().map(n -> new PassengerDto(n.firstName(), n.lastName())).toList(),
                p.getSegments().stream().map(s -> new SegmentDto(s.flightNumber(), s.departureDate(), s.origin(), s.destination())).toList(),
                p.getTicketNumbers());
    }
}
