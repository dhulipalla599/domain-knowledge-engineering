package com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.domain;

import java.util.List;

/** Domain events. Each maps to a Kafka topic in production (see TOPIC_* constants). */
public final class SupportEvents {
    public static final String TOPIC_ANSWERED = "support.query.answered";
    public static final String TOPIC_ESCALATED = "support.query.escalated";
    public static final String TOPIC_RESOLVED = "support.query.resolved";

    private SupportEvents() {}

    public record QueryAnswered(Long queryId, String customerId, List<Long> citedArticleIds, double confidence) {}

    public record QueryEscalated(Long queryId, String customerId, String reason) {}

    public record QueryResolved(Long queryId, String customerId) {}
}
