package com.dke.insurance.underwriting.integration;

import com.dke.insurance.underwriting.domain.UnderwritingEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Stand-in for Kafka: records every event with its topic. Production: a KafkaTemplate publisher. */
@Component
public class UnderwritingEventListener {

    private static final Logger log = LoggerFactory.getLogger(UnderwritingEventListener.class);
    private final List<UnderwritingEvent> published = new CopyOnWriteArrayList<>();

    @EventListener
    public void on(UnderwritingEvent event) {
        published.add(event);
        log.info("publish topic={} key={} payload={}", event.topic(), event.applicationId(), event);
    }

    public List<UnderwritingEvent> published() {
        return List.copyOf(published);
    }
}
