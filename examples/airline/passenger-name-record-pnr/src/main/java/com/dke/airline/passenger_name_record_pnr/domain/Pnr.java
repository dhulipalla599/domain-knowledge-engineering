package com.dke.airline.passenger_name_record_pnr.domain;

import com.dke.airline.passenger_name_record_pnr.domain.RuleViolationException.Kind;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Entity
@Table(name = "pnr")
public class Pnr {

    public static final int MAX_PASSENGERS = 9;
    private static final Pattern NAME = Pattern.compile("^[A-Z][A-Z '-]*$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 6)
    private String recordLocator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PnrStatus status;

    @Column(nullable = false)
    private String contactEmail;

    @Column(nullable = false)
    private Instant ticketTimeLimit;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "pnr_passenger", joinColumns = @JoinColumn(name = "pnr_id"))
    private List<PassengerName> passengers = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "pnr_segment", joinColumns = @JoinColumn(name = "pnr_id"))
    private List<FlightSegment> segments = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "pnr_ticket", joinColumns = @JoinColumn(name = "pnr_id"))
    @Column(name = "ticket_number")
    private List<String> ticketNumbers = new ArrayList<>();

    protected Pnr() {
    }

    private Pnr(String recordLocator, String contactEmail, Instant ticketTimeLimit,
                List<PassengerName> passengers, List<FlightSegment> segments) {
        this.recordLocator = recordLocator;
        this.contactEmail = contactEmail;
        this.ticketTimeLimit = ticketTimeLimit;
        this.passengers = passengers;
        this.segments = segments;
        this.status = PnrStatus.ON_HOLD;
    }

    /** Builds a new ON_HOLD PNR after validating and normalising passenger names. */
    public static Pnr open(String recordLocator, String contactEmail, Instant ticketTimeLimit,
                           List<PassengerName> rawPassengers, List<FlightSegment> segments) {
        if (rawPassengers.isEmpty() || rawPassengers.size() > MAX_PASSENGERS) {
            throw new RuleViolationException(Kind.INVALID,
                    "A PNR must hold between 1 and " + MAX_PASSENGERS + " passengers");
        }
        if (segments.isEmpty()) {
            throw new RuleViolationException(Kind.INVALID, "A PNR needs at least one flight segment");
        }
        List<PassengerName> passengers = rawPassengers.stream().map(Pnr::normalise).toList();
        return new Pnr(recordLocator, contactEmail, ticketTimeLimit,
                new ArrayList<>(passengers), new ArrayList<>(segments));
    }

    static PassengerName normalise(PassengerName raw) {
        PassengerName name = new PassengerName(raw.firstName().trim().toUpperCase(), raw.lastName().trim().toUpperCase());
        if (!NAME.matcher(name.firstName()).matches() || !NAME.matcher(name.lastName()).matches()) {
            throw new RuleViolationException(Kind.INVALID,
                    "Names must use letters, spaces, hyphens or apostrophes only: " + raw.firstName() + " " + raw.lastName());
        }
        return name;
    }

    /** Rule: ticketing is only possible while ON_HOLD and before the ticketing time limit. */
    public void ensureTicketable(Instant now) {
        if (status != PnrStatus.ON_HOLD) {
            throw new RuleViolationException(Kind.CONFLICT, "PNR " + recordLocator + " is " + status + ", cannot be ticketed");
        }
        if (now.isAfter(ticketTimeLimit)) {
            throw new RuleViolationException(Kind.CONFLICT,
                    "Ticketing time limit " + ticketTimeLimit + " has passed for PNR " + recordLocator);
        }
    }

    public void markTicketed(List<String> numbers) {
        transitionTo(PnrStatus.TICKETED);
        this.ticketNumbers = new ArrayList<>(numbers);
    }

    public void cancel() {
        transitionTo(PnrStatus.CANCELLED);
    }

    private void transitionTo(PnrStatus next) {
        if (!status.allowedNext().contains(next)) {
            throw new RuleViolationException(Kind.CONFLICT, "Illegal transition " + status + " -> " + next);
        }
        this.status = next;
    }

    public Long getId() { return id; }
    public String getRecordLocator() { return recordLocator; }
    public PnrStatus getStatus() { return status; }
    public String getContactEmail() { return contactEmail; }
    public Instant getTicketTimeLimit() { return ticketTimeLimit; }
    public List<PassengerName> getPassengers() { return List.copyOf(passengers); }
    public List<FlightSegment> getSegments() { return List.copyOf(segments); }
    public List<String> getTicketNumbers() { return List.copyOf(ticketNumbers); }
}
