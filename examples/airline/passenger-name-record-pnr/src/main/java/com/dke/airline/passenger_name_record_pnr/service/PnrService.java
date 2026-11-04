package com.dke.airline.passenger_name_record_pnr.service;

import com.dke.airline.passenger_name_record_pnr.domain.*;
import com.dke.airline.passenger_name_record_pnr.domain.PnrEvents.*;
import com.dke.airline.passenger_name_record_pnr.domain.RuleViolationException.Kind;
import com.dke.airline.passenger_name_record_pnr.integration.RecordLocatorGenerator;
import com.dke.airline.passenger_name_record_pnr.integration.TicketingGateway;
import com.dke.airline.passenger_name_record_pnr.repository.PnrRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PnrService {

    static final Duration TICKETING_WINDOW = Duration.ofHours(24);

    private final PnrRepository repository;
    private final RecordLocatorGenerator locators;
    private final TicketingGateway ticketing;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public PnrService(PnrRepository repository, RecordLocatorGenerator locators, TicketingGateway ticketing,
                      ApplicationEventPublisher events, Clock clock) {
        this.repository = repository;
        this.locators = locators;
        this.ticketing = ticketing;
        this.events = events;
        this.clock = clock;
    }

    /** Creates an ON_HOLD PNR. Rejects a passenger who already holds an active booking on the same flight and date. */
    @Transactional
    public Pnr create(String contactEmail, List<PassengerName> passengers, List<FlightSegment> segments) {
        Instant now = clock.instant();
        Pnr pnr = Pnr.open(locators.next(), contactEmail, now.plus(TICKETING_WINDOW), passengers, segments);
        for (PassengerName pax : pnr.getPassengers()) {
            for (FlightSegment seg : pnr.getSegments()) {
                if (repository.existsActiveBooking(pax.firstName(), pax.lastName(),
                        seg.flightNumber(), seg.departureDate(), PnrStatus.CANCELLED)) {
                    throw new RuleViolationException(Kind.CONFLICT, "Duplicate booking: " + pax.firstName() + " "
                            + pax.lastName() + " already holds " + seg.flightNumber() + " on " + seg.departureDate());
                }
            }
        }
        repository.save(pnr);
        events.publishEvent(new PnrCreated(pnr.getRecordLocator(), pnr.getPassengers().size(), pnr.getTicketTimeLimit()));
        return pnr;
    }

    @Transactional(readOnly = true)
    public Pnr get(String recordLocator) {
        return repository.findByRecordLocator(recordLocator.toUpperCase())
                .orElseThrow(() -> new NotFoundException("No PNR with record locator " + recordLocator));
    }

    /** Issues tickets if the PNR is ON_HOLD and within its ticketing time limit; idempotent retries get 409. */
    @Transactional
    public Pnr ticket(String recordLocator) {
        Pnr pnr = get(recordLocator);
        pnr.ensureTicketable(clock.instant());
        pnr.markTicketed(ticketing.issueTickets(pnr));
        events.publishEvent(new PnrTicketed(pnr.getRecordLocator(), pnr.getTicketNumbers()));
        return pnr;
    }

    @Transactional
    public Pnr cancel(String recordLocator, String reason) {
        Pnr pnr = get(recordLocator);
        pnr.cancel();
        events.publishEvent(new PnrCancelled(pnr.getRecordLocator(), reason));
        return pnr;
    }
}
