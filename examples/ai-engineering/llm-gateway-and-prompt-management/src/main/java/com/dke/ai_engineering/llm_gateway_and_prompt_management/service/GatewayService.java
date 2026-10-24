package com.dke.ai_engineering.llm_gateway_and_prompt_management.service;

import com.dke.ai_engineering.llm_gateway_and_prompt_management.domain.GatewayCall;
import com.dke.ai_engineering.llm_gateway_and_prompt_management.domain.LlmCallCompleted;
import com.dke.ai_engineering.llm_gateway_and_prompt_management.domain.PromptStatus;
import com.dke.ai_engineering.llm_gateway_and_prompt_management.domain.PromptVersion;
import com.dke.ai_engineering.llm_gateway_and_prompt_management.domain.TeamBudget;
import com.dke.ai_engineering.llm_gateway_and_prompt_management.integration.LlmClient;
import com.dke.ai_engineering.llm_gateway_and_prompt_management.integration.LlmResponse;
import com.dke.ai_engineering.llm_gateway_and_prompt_management.integration.ProviderUnavailableException;
import com.dke.ai_engineering.llm_gateway_and_prompt_management.repository.GatewayCallRepository;
import com.dke.ai_engineering.llm_gateway_and_prompt_management.repository.PromptVersionRepository;
import com.dke.ai_engineering.llm_gateway_and_prompt_management.repository.TeamBudgetRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GatewayService {

    private final PromptVersionRepository prompts;
    private final TeamBudgetRepository budgets;
    private final GatewayCallRepository calls;
    private final PiiRedactor redactor;
    private final List<LlmClient> clients;
    private final ApplicationEventPublisher events;

    public GatewayService(PromptVersionRepository prompts, TeamBudgetRepository budgets,
                          GatewayCallRepository calls, PiiRedactor redactor,
                          List<LlmClient> clients, ApplicationEventPublisher events) {
        this.prompts = prompts;
        this.budgets = budgets;
        this.calls = calls;
        this.redactor = redactor;
        this.clients = clients;
        this.events = events;
    }

    @Transactional
    public GatewayCall complete(String teamId, String promptName, Map<String, String> variables,
                                int maxTokens, String idempotencyKey) {
        if (idempotencyKey != null) {
            var earlier = calls.findByTeamIdAndIdempotencyKey(teamId, idempotencyKey);
            if (earlier.isPresent()) {
                return earlier.get();
            }
        }
        PromptVersion prompt = prompts.findByNameAndStatus(promptName, PromptStatus.ACTIVE)
                .orElseThrow(() -> new BusinessRuleException(HttpStatus.CONFLICT, "No active prompt",
                        "Prompt '%s' has no ACTIVE version".formatted(promptName)));
        TeamBudget budget = budgets.lockByTeamId(teamId)
                .orElseThrow(() -> new BusinessRuleException(HttpStatus.NOT_FOUND, "Unknown team",
                        "No token budget for team " + teamId));

        int redactions = 0;
        Map<String, String> safe = new LinkedHashMap<>();
        for (var entry : variables.entrySet()) {
            PiiRedactor.Redacted redacted = redactor.redact(entry.getValue());
            safe.put(entry.getKey(), redacted.text());
            redactions += redacted.count();
        }
        String rendered = prompt.render(safe);

        long worstCase = LlmClient.estimateTokens(rendered) + maxTokens;
        if (!budget.canAfford(worstCase)) {
            throw new BusinessRuleException(HttpStatus.TOO_MANY_REQUESTS, "Token budget exceeded",
                    "Team %s has %d tokens left but this call may need %d"
                            .formatted(teamId, budget.remaining(), worstCase));
        }

        LlmResponse response = callWithFailover(rendered, maxTokens);
        GatewayCall call = calls.save(new GatewayCall(teamId, promptName, prompt.getVersionNo(),
                response.provider(), response.promptTokens(), response.completionTokens(),
                redactions, idempotencyKey, response.text()));
        budget.charge(call.totalTokens());
        events.publishEvent(new LlmCallCompleted(call.getId(), teamId, promptName,
                call.getPromptVersion(), call.getProvider(), call.totalTokens()));
        return call;
    }

    private LlmResponse callWithFailover(String prompt, int maxTokens) {
        for (LlmClient client : clients) {
            try {
                return client.complete(prompt, maxTokens);
            } catch (ProviderUnavailableException e) {
                // try the next provider in order
            }
        }
        throw new BusinessRuleException(HttpStatus.SERVICE_UNAVAILABLE, "No provider available",
                "Every configured model provider is down");
    }
}
