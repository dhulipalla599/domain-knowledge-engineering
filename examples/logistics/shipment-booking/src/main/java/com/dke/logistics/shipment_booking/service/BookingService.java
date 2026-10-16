package com.dke.logistics.shipment_booking.service;

import com.dke.logistics.shipment_booking.domain.Booking;
import com.dke.logistics.shipment_booking.domain.BookingEvent;
import com.dke.logistics.shipment_booking.domain.RateQuote;
import com.dke.logistics.shipment_booking.integration.CarrierGateway;
import com.dke.logistics.shipment_booking.integration.CarrierUnavailableException;
import com.dke.logistics.shipment_booking.repository.BookingRepository;
import com.dke.logistics.shipment_booking.repository.RateQuoteRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    private final BookingRepository bookings;
    private final RateQuoteRepository quotes;
    private final CarrierGateway carrier;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public BookingService(BookingRepository bookings, RateQuoteRepository quotes, CarrierGateway carrier,
                          ApplicationEventPublisher events, Clock clock) {
        this.bookings = bookings;
        this.quotes = quotes;
        this.carrier = carrier;
        this.events = events;
        this.clock = clock;
    }

    /** Books a shipment on a quote. Replaying the same idempotency key returns the original booking. */
    @Transactional
    public Booking book(String idempotencyKey, String customerId, String quoteId, double weightKg, String unNumber) {
        var existing = bookings.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }
        RateQuote quote = quotes.findById(quoteId)
                .orElseThrow(() -> new BookingRuleException(HttpStatus.NOT_FOUND, "Unknown quote " + quoteId));
        Instant now = clock.instant();
        if (quote.isExpiredAt(now)) {
            throw new BookingRuleException(HttpStatus.GONE, "Quote " + quoteId + " expired at " + quote.getValidUntil());
        }
        if (weightKg > quote.getMaxWeightKg()) {
            throw new BookingRuleException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Weight " + weightKg + " kg exceeds the quote limit of " + quote.getMaxWeightKg() + " kg");
        }
        boolean dangerous = unNumber != null && !unNumber.isBlank();
        if (dangerous && !quote.isAcceptsDangerousGoods()) {
            throw new BookingRuleException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Carrier " + quote.getCarrier() + " does not accept dangerous goods (" + unNumber + ")");
        }

        var booking = new Booking("BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                customerId, quote, idempotencyKey, weightKg, dangerous ? unNumber : null, now);
        bookings.save(booking);
        events.publishEvent(new BookingEvent.BookingRequested(booking.getId(), booking.getReference(), customerId));
        try {
            booking.confirm(carrier.requestSlot(quote, booking));
            events.publishEvent(new BookingEvent.BookingConfirmed(
                    booking.getId(), booking.getReference(), booking.getCarrierConfirmation()));
        } catch (CarrierUnavailableException e) {
            throw new BookingRuleException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        }
        return booking;
    }

    @Transactional
    public Booking cancel(UUID id) {
        Booking booking = bookings.findById(id)
                .orElseThrow(() -> new BookingRuleException(HttpStatus.NOT_FOUND, "Unknown booking " + id));
        try {
            booking.cancel();
        } catch (IllegalStateException e) {
            throw new BookingRuleException(HttpStatus.CONFLICT, e.getMessage());
        }
        events.publishEvent(new BookingEvent.BookingCancelled(booking.getId(), booking.getReference()));
        return booking;
    }

    @Transactional(readOnly = true)
    public Booking get(UUID id) {
        return bookings.findById(id)
                .orElseThrow(() -> new BookingRuleException(HttpStatus.NOT_FOUND, "Unknown booking " + id));
    }
}
