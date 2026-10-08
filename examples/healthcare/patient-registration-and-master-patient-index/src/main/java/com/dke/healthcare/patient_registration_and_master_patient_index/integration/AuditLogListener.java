package com.dke.healthcare.patient_registration_and_master_patient_index.integration;

import com.dke.healthcare.patient_registration_and_master_patient_index.domain.DomainEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Stand-in for a Kafka consumer group that writes the compliance audit trail.
 * Production: consume the topics and append to an immutable audit store, deduplicating on event id.
 */
@Component
public class AuditLogListener {

    private static final Logger log = LoggerFactory.getLogger(AuditLogListener.class);
    private final List<String> entries = new CopyOnWriteArrayList<>();

    @EventListener
    public void on(DomainEvent event) {
        entries.add(event.topic() + ":" + event.key());
        log.info("audit topic={} key={} payload={}", event.topic(), event.key(), event);
    }

    public List<String> entries() {
        return List.copyOf(entries);
    }
}
