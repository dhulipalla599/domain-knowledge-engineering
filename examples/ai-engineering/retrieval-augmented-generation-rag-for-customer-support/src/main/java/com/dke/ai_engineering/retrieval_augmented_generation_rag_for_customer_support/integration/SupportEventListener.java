package com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.integration;

import com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.domain.SupportEvents.QueryAnswered;
import com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.domain.SupportEvents.QueryEscalated;
import com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.domain.SupportEvents.QueryResolved;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * In-process stand-in for the Kafka consumers: a help-desk consumer on support.query.escalated and an analytics
 * consumer on support.query.answered / support.query.resolved.
 */
@Component
public class SupportEventListener {

    private static final Logger log = LoggerFactory.getLogger(SupportEventListener.class);

    private final List<QueryEscalated> openTickets = new CopyOnWriteArrayList<>();
    private final AtomicInteger deflected = new AtomicInteger();

    @EventListener
    void onAnswered(QueryAnswered event) {
        log.info("support.query.answered id={} cited={} confidence={}", event.queryId(), event.citedArticleIds(), event.confidence());
    }

    @EventListener
    void onEscalated(QueryEscalated event) {
        openTickets.add(event);
        log.info("support.query.escalated id={} reason={}", event.queryId(), event.reason());
    }

    @EventListener
    void onResolved(QueryResolved event) {
        deflected.incrementAndGet();
        log.info("support.query.resolved id={}", event.queryId());
    }

    public List<QueryEscalated> openTickets() { return List.copyOf(openTickets); }

    public int deflectedCount() { return deflected.get(); }
}
