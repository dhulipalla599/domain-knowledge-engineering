package com.dke.ai_engineering.llm_gateway_and_prompt_management.integration;

import com.dke.ai_engineering.llm_gateway_and_prompt_management.domain.LlmCallCompleted;
import com.dke.ai_engineering.llm_gateway_and_prompt_management.domain.PromptActivated;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Consumer of the two domain events: keeps an audit trail and per-team spend (a Kafka consumer in production). */
@Component
public class AuditTrailListener {

    private final List<String> entries = new CopyOnWriteArrayList<>();
    private final Map<String, Long> tokensByTeam = new ConcurrentHashMap<>();

    @EventListener
    void on(PromptActivated event) {
        entries.add("prompt.version.activated %s v%d".formatted(event.name(), event.versionNo()));
    }

    @EventListener
    void on(LlmCallCompleted event) {
        entries.add("llm.call.completed %s via %s".formatted(event.teamId(), event.provider()));
        tokensByTeam.merge(event.teamId(), (long) event.totalTokens(), Long::sum);
    }

    public List<String> entries() {
        return List.copyOf(entries);
    }

    public long tokensFor(String teamId) {
        return tokensByTeam.getOrDefault(teamId, 0L);
    }
}
