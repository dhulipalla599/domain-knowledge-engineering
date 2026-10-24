package com.dke.ai_engineering.llm_gateway_and_prompt_management.api;

import com.dke.ai_engineering.llm_gateway_and_prompt_management.api.ApiModels.*;
import com.dke.ai_engineering.llm_gateway_and_prompt_management.repository.TeamBudgetRepository;
import com.dke.ai_engineering.llm_gateway_and_prompt_management.service.BusinessRuleException;
import com.dke.ai_engineering.llm_gateway_and_prompt_management.service.GatewayService;
import com.dke.ai_engineering.llm_gateway_and_prompt_management.service.PromptService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class GatewayController {

    private static final int DEFAULT_MAX_TOKENS = 256;

    private final GatewayService gateway;
    private final PromptService promptService;
    private final TeamBudgetRepository budgets;

    public GatewayController(GatewayService gateway, PromptService promptService, TeamBudgetRepository budgets) {
        this.gateway = gateway;
        this.promptService = promptService;
        this.budgets = budgets;
    }

    @PostMapping("/gateway/completions")
    public CompletionResponse complete(@Valid @RequestBody CompletionRequest request,
                                       @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        int maxTokens = request.maxTokens() == null ? DEFAULT_MAX_TOKENS : request.maxTokens();
        return CompletionResponse.of(gateway.complete(request.teamId(), request.promptName(),
                request.variables(), maxTokens, key));
    }

    @PostMapping("/prompts")
    @ResponseStatus(HttpStatus.CREATED)
    public PromptResponse createDraft(@Valid @RequestBody CreatePromptRequest request) {
        return PromptResponse.of(promptService.createDraft(request.name(), request.template(), request.author()));
    }

    @PostMapping("/prompts/{id}/approve")
    public PromptResponse approve(@PathVariable Long id, @Valid @RequestBody ApproveRequest request) {
        return PromptResponse.of(promptService.approve(id, request.approver()));
    }

    @PostMapping("/prompts/{id}/activate")
    public PromptResponse activate(@PathVariable Long id) {
        return PromptResponse.of(promptService.activate(id));
    }

    @GetMapping("/prompts")
    public List<PromptResponse> history(@RequestParam String name) {
        return promptService.history(name).stream().map(PromptResponse::of).toList();
    }

    @GetMapping("/budgets/{teamId}")
    public BudgetResponse budget(@PathVariable String teamId) {
        return budgets.findByTeamId(teamId).map(BudgetResponse::of)
                .orElseThrow(() -> new BusinessRuleException(HttpStatus.NOT_FOUND, "Unknown team",
                        "No token budget for team " + teamId));
    }
}
