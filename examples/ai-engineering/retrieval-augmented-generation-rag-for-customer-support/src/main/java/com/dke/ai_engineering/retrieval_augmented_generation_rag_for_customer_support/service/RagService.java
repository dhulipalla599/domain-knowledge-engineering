package com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.service;

import com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.domain.QueryStatus;
import com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.domain.SupportEvents.QueryAnswered;
import com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.domain.SupportEvents.QueryEscalated;
import com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.domain.SupportEvents.QueryResolved;
import com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.domain.SupportQuery;
import com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.integration.LlmClient;
import com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.repository.SupportQueryRepository;
import com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.service.Retriever.Passage;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RagService {

    public static class PromptInjectionException extends RuntimeException {
        public PromptInjectionException() { super("The question looks like an attempt to override the assistant's instructions"); }
    }

    public static class IdempotencyConflictException extends RuntimeException {
        public IdempotencyConflictException(String key) { super("Idempotency-Key '" + key + "' was already used with a different question"); }
    }

    private static final Pattern INJECTION = Pattern.compile(
            "(?i)ignore (all |any )?(previous|prior|above) instructions|reveal (your )?system prompt");
    private static final Pattern CITATION = Pattern.compile("\\[KB-(\\d+)]");

    private final Retriever retriever;
    private final LlmClient llm;
    private final PiiRedactor redactor;
    private final SupportQueryRepository queries;
    private final ApplicationEventPublisher events;
    private final double minScore;
    private final int topK;

    public RagService(Retriever retriever, LlmClient llm, PiiRedactor redactor, SupportQueryRepository queries,
                      ApplicationEventPublisher events,
                      @Value("${rag.min-score:0.25}") double minScore, @Value("${rag.top-k:3}") int topK) {
        this.retriever = retriever;
        this.llm = llm;
        this.redactor = redactor;
        this.queries = queries;
        this.events = events;
        this.minScore = minScore;
        this.topK = topK;
    }

    /** Core flow: guard, redact, retrieve, generate, verify grounding, then answer or escalate to a human. */
    @Transactional
    public SupportQuery ask(String customerId, String rawQuestion, String idempotencyKey) {
        if (INJECTION.matcher(rawQuestion).find()) {
            throw new PromptInjectionException();
        }
        String question = redactor.redact(rawQuestion).trim();
        if (idempotencyKey != null) {
            var existing = queries.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                SupportQuery q = existing.get();
                if (!q.getCustomerId().equals(customerId) || !q.getQuestion().equals(question)) {
                    throw new IdempotencyConflictException(idempotencyKey);
                }
                return q;
            }
        }
        SupportQuery query = queries.save(new SupportQuery(customerId, question, idempotencyKey));

        List<Passage> passages = retriever.retrieve(question, topK);
        if (passages.isEmpty() || passages.get(0).score() < minScore) {
            return escalate(query, "No sufficiently relevant knowledge-base article");
        }
        String answer = llm.complete(buildPrompt(question, passages));
        List<Long> cited = validCitations(answer, passages);
        if (cited.isEmpty()) {
            return escalate(query, "Answer was not grounded in the retrieved sources");
        }
        query.markAnswered(answer, cited, passages.get(0).score());
        events.publishEvent(new QueryAnswered(query.getId(), customerId, cited, passages.get(0).score()));
        return query;
    }

    /** Customer feedback: a helpful answer deflects the ticket, an unhelpful one goes to a human. */
    @Transactional
    public SupportQuery feedback(Long queryId, boolean helpful) {
        SupportQuery query = queries.findById(queryId)
                .orElseThrow(() -> new NoSuchElementException("Query " + queryId + " not found"));
        if (helpful) {
            query.resolve();
            events.publishEvent(new QueryResolved(query.getId(), query.getCustomerId()));
            return query;
        }
        return escalate(query, "Customer marked the answer as unhelpful");
    }

    @Transactional(readOnly = true)
    public SupportQuery get(Long queryId) {
        return queries.findById(queryId).orElseThrow(() -> new NoSuchElementException("Query " + queryId + " not found"));
    }

    private SupportQuery escalate(SupportQuery query, String reason) {
        query.escalate(reason);
        events.publishEvent(new QueryEscalated(query.getId(), query.getCustomerId(), reason));
        return query;
    }

    public static String buildPrompt(String question, List<Passage> passages) {
        StringBuilder sb = new StringBuilder("""
                You are the Brightwave Mobile support assistant. Answer ONLY from the sources below.
                Cite every claim as [KB-<id>]. If the sources do not answer the question, say "I don't know."
                SOURCES:
                """);
        for (Passage p : passages) {
            sb.append("[KB-").append(p.articleId()).append("] ").append(p.title()).append(" - ")
                    .append(p.text().replace('\n', ' ')).append('\n');
        }
        return sb.append("QUESTION: ").append(question).toString();
    }

    /** Every citation must point at a retrieved passage; an empty or invented citation returns an empty list. */
    public static List<Long> validCitations(String answer, List<Passage> passages) {
        Set<Long> allowed = new java.util.HashSet<>();
        passages.forEach(p -> allowed.add(p.articleId()));
        List<Long> found = new ArrayList<>();
        Matcher m = CITATION.matcher(answer);
        while (m.find()) {
            Long id = Long.valueOf(m.group(1));
            if (!allowed.contains(id)) return List.of();
            if (!found.contains(id)) found.add(id);
        }
        return found;
    }
}
