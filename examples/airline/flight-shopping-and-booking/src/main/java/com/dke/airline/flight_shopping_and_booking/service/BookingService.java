package com.dke.airline.flight_shopping_and_booking.service;

import com.dke.airline.flight_shopping_and_booking.domain.Booking;
import com.dke.airline.flight_shopping_and_booking.domain.BookingEvents.BookingHeld;
import com.dke.airline.flight_shopping_and_booking.domain.BookingEvents.BookingReleased;
import com.dke.airline.flight_shopping_and_booking.domain.BookingEvents.BookingTicketed;
import com.dke.airline.flight_shopping_and_booking.domain.BookingStatus;
import com.dke.airline.flight_shopping_and_booking.domain.FlightOffer;
import com.dke.airline.flight_shopping_and_booking.integration.PaymentGateway;
import com.dke.airline.flight_shopping_and_booking.integration.TicketIssuer;
import com.dke.airline.flight_shopping_and_booking.repository.BookingRepository;
import com.dke.airline.flight_shopping_and_booking.repository.FlightOfferRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    static final Duration HOLD_DURATION = Duration.ofMinutes(15);

    public record HoldResult(Booking booking, boolean created) {
    }

    private final FlightOfferRepository offers;
    private final BookingRepository bookings;
    private final PaymentGateway payments;
    private final TicketIssuer tickets;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public BookingService(FlightOfferRepository offers, BookingRepository bookings, PaymentGateway payments,
                          TicketIssuer tickets, ApplicationEventPublisher events, Clock clock) {
        this.offers = offers;
        this.bookings = bookings;
        this.payments = payments;
        this.tickets = tickets;
        this.events = events;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<FlightOffer> search(String origin, String destination, LocalDate date) {
        return offers.findByOriginAndDestinationAndDepartureDateAndSeatsAvailableGreaterThanOrderByPriceAsc(
                origin.toUpperCase(), destination.toUpperCase(), date, 0);
    }

    /**
     * Key business rule: replay-safe hold. The same Idempotency-Key returns the same booking, the fare the
     * customer saw must still be the fare on sale, and a seat is taken from inventory in the same transaction.
     */
    @Transactional
    public HoldResult hold(String idempotencyKey, UUID offerId, String passengerName, String passengerEmail,
                           BigDecimal expectedPrice) {
        var existing = bookings.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return new HoldResult(existing.get(), false);
        }
        var offer = offers.findById(offerId).orElseThrow(() -> new NotFoundException("Offer", offerId));
        if (offer.getPrice().compareTo(expectedPrice) != 0) {
            throw new BusinessRuleException("Fare changed from " + expectedPrice + " to " + offer.getPrice()
                    + " " + offer.getCurrency() + "; search again to confirm the new price");
        }
        try {
            offer.holdSeat();
        } catch (IllegalStateException e) {
            throw new BusinessRuleException(e.getMessage());
        }
        Instant now = Instant.now(clock);
        var booking = bookings.save(new Booking(idempotencyKey, offerId, passengerName, passengerEmail,
                offer.getPrice(), offer.getCurrency(), now, now.plus(HOLD_DURATION)));
        events.publishEvent(new BookingHeld(booking.getId(), offerId, booking.getTotalAmount(),
                booking.getCurrency(), now));
        return new HoldResult(booking, true);
    }

    /** Pays and tickets a held booking. An expired hold is released and persisted even though the call fails. */
    @Transactional(noRollbackFor = BusinessRuleException.class)
    public Booking confirm(UUID bookingId, String paymentToken) {
        var booking = get(bookingId);
        Instant now = Instant.now(clock);
        if (booking.isHoldExpired(now)) {
            expireAndRelease(booking);
            throw new BusinessRuleException("Hold expired at " + booking.getHoldExpiresAt() + "; search again");
        }
        if (booking.getStatus() != BookingStatus.HELD) {
            throw new IllegalStateException("Only a HELD booking can be confirmed, not " + booking.getStatus());
        }
        if (!payments.authorize(paymentToken, booking.getTotalAmount(), booking.getCurrency())) {
            throw new BusinessRuleException("Payment declined; the seat stays held until "
                    + booking.getHoldExpiresAt());
        }
        booking.confirmPayment();
        var ticket = tickets.issue(bookingId, booking.getPassengerName());
        booking.issueTicket(ticket.pnr(), ticket.ticketNumber());
        events.publishEvent(new BookingTicketed(bookingId, ticket.pnr(), ticket.ticketNumber(), now));
        return booking;
    }

    @Transactional
    public Booking cancel(UUID bookingId, String reason) {
        var booking = get(bookingId);
        booking.cancel(reason);
        offers.findById(booking.getOfferId()).ifPresent(FlightOffer::releaseSeat);
        publishReleased(booking);
        return booking;
    }

    /** Sweep run by a scheduler in production: expires every hold past its deadline and frees the seats. */
    @Transactional
    public int expireStaleHolds() {
        var stale = bookings.findByStatusAndHoldExpiresAtBefore(BookingStatus.HELD, Instant.now(clock));
        stale.forEach(booking -> expireAndRelease(booking));
        return stale.size();
    }

    @Transactional(readOnly = true)
    public Booking get(UUID id) {
        return bookings.findById(id).orElseThrow(() -> new NotFoundException("Booking", id));
    }

    private void expireAndRelease(Booking booking) {
        booking.expire();
        offers.findById(booking.getOfferId()).ifPresent(FlightOffer::releaseSeat);
        publishReleased(booking);
    }

    private void publishReleased(Booking booking) {
        events.publishEvent(new BookingReleased(booking.getId(), booking.getStatus(),
                booking.getStatusReason(), Instant.now(clock)));
    }
}
