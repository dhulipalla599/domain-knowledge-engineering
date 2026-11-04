package com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "support_query")
public class SupportQuery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String customerId;

    @Column(unique = true)
    private String idempotencyKey;

    /** Stored already redacted: raw PII never reaches the database or the model. */
    @Column(nullable = false, length = 1000)
    private String question;

    @Column(length = 2000)
    private String answer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QueryStatus status = QueryStatus.RECEIVED;

    private Double confidence;
    private String escalationReason;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "support_query_citation")
    private List<Long> citedArticleIds = new ArrayList<>();

    private Instant createdAt = Instant.now();

    protected SupportQuery() {}

    public SupportQuery(String customerId, String question, String idempotencyKey) {
        this.customerId = customerId;
        this.question = question;
        this.idempotencyKey = idempotencyKey;
    }

    public void markAnswered(String answer, List<Long> citedArticleIds, double confidence) {
        moveTo(QueryStatus.ANSWERED);
        this.answer = answer;
        this.citedArticleIds = new ArrayList<>(citedArticleIds);
        this.confidence = confidence;
    }

    public void escalate(String reason) {
        moveTo(QueryStatus.ESCALATED);
        this.escalationReason = reason;
    }

    public void resolve() {
        moveTo(QueryStatus.RESOLVED);
    }

    private void moveTo(QueryStatus next) {
        if (!status.canMoveTo(next)) {
            throw new IllegalStateException("Query " + id + " cannot move from " + status + " to " + next);
        }
        this.status = next;
    }

    public Long getId() { return id; }
    public String getCustomerId() { return customerId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getQuestion() { return question; }
    public String getAnswer() { return answer; }
    public QueryStatus getStatus() { return status; }
    public Double getConfidence() { return confidence; }
    public String getEscalationReason() { return escalationReason; }
    public List<Long> getCitedArticleIds() { return citedArticleIds; }
    public Instant getCreatedAt() { return createdAt; }
}
