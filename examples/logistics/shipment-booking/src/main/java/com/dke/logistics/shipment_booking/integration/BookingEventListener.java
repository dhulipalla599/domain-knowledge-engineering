package com.dke.logistics.shipment_booking.integration;

import com.dke.logistics.shipment_booking.domain.BookingEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Stand-in for Kafka consumers (notifications, billing, track-and-trace). Keeps an in-memory audit trail. */
@Component
public class BookingEventListener {

    private static final Logger log = LoggerFactory.getLogger(BookingEventListener.class);
    private final List<String> published = new CopyOnWriteArrayList<>();

    @EventListener
    public void on(BookingEvent event) {
        published.add(event.topic());
        log.info("[{}] booking={}", event.topic(), event.bookingId());
    }

    public List<String> publishedTopics() {
        return List.copyOf(published);
    }
}
