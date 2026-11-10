package com.dke.insurance.policy_administration.integration;

import com.dke.insurance.policy_administration.domain.PolicyEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Stand-in for Kafka consumers (billing, documents, reinsurance): records and logs each topic message. */
@Component
public class PolicyEventListener {

    private static final Logger log = LoggerFactory.getLogger(PolicyEventListener.class);
    private final List<String> published = new CopyOnWriteArrayList<>();

    @EventListener
    public void on(PolicyEvent event) {
        published.add(event.topic() + ":" + event.policyId());
        log.info("topic={} key={} payload={}", event.topic(), event.policyId(), event);
    }

    public List<String> published() {
        return List.copyOf(published);
    }
}
