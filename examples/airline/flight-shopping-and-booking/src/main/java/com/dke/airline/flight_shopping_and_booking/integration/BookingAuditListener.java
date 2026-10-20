package com.dke.airline.flight_shopping_and_booking.integration;

import com.dke.airline.flight_shopping_and_booking.domain.BookingEvents;
import com.dke.airline.flight_shopping_and_booking.domain.BookingEvents.BookingHeld;
import com.dke.airline.flight_shopping_and_booking.domain.BookingEvents.BookingReleased;
import com.dke.airline.flight_shopping_and_booking.domain.BookingEvents.BookingTicketed;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Consumer stand-in that records every event, like an itinerary-email or audit service reading the Kafka topics. */
@Component
public class BookingAuditListener {

    private static final Logger log = LoggerFactory.getLogger(BookingAuditListener.class);
    private final List<String> entries = new CopyOnWriteArrayList<>();

    @EventListener
    void on(BookingHeld event) {
        record(BookingEvents.HELD_TOPIC, event.bookingId() + " amount=" + event.amount() + " " + event.currency());
    }

    @EventListener
    void on(BookingTicketed event) {
        record(BookingEvents.TICKETED_TOPIC, event.bookingId() + " pnr=" + event.pnr());
    }

    @EventListener
    void on(BookingReleased event) {
        record(BookingEvents.RELEASED_TOPIC, event.bookingId() + " status=" + event.finalStatus());
    }

    public List<String> entries() {
        return List.copyOf(entries);
    }

    private void record(String topic, String detail) {
        String line = topic + " | " + detail;
        entries.add(line);
        log.info("audit {}", line);
    }
}
