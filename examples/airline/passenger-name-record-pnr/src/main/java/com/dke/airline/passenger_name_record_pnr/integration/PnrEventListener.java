package com.dke.airline.passenger_name_record_pnr.integration;

import com.dke.airline.passenger_name_record_pnr.domain.PnrEvents.PnrCancelled;
import com.dke.airline.passenger_name_record_pnr.domain.PnrEvents.PnrCreated;
import com.dke.airline.passenger_name_record_pnr.domain.PnrEvents.PnrTicketed;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Stand-in for Kafka consumers (notification, audit, revenue accounting): records what it saw. */
@Component
public class PnrEventListener {

    private static final Logger log = LoggerFactory.getLogger(PnrEventListener.class);
    private final List<String> audit = new CopyOnWriteArrayList<>();

    @EventListener
    void onCreated(PnrCreated e) {
        record("pnr.created", e.recordLocator(), e);
    }

    @EventListener
    void onTicketed(PnrTicketed e) {
        record("pnr.ticketed", e.recordLocator(), e);
    }

    @EventListener
    void onCancelled(PnrCancelled e) {
        record("pnr.cancelled", e.recordLocator(), e);
    }

    private void record(String topic, String key, Object event) {
        audit.add(topic + ":" + key);
        log.info("[{}] key={} {}", topic, key, event);
    }

    public List<String> audit() {
        return List.copyOf(audit);
    }
}
