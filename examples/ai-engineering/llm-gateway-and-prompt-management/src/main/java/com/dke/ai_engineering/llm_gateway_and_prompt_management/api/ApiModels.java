package com.dke.ai_engineering.llm_gateway_and_prompt_management.api;

import com.dke.ai_engineering.llm_gateway_and_prompt_management.domain.GatewayCall;
import com.dke.ai_engineering.llm_gateway_and_prompt_management.domain.PromptVersion;
import com.dke.ai_engineering.llm_gateway_and_prompt_management.domain.TeamBudget;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public final class ApiModels {

    private ApiModels() {
    }

    public record CreatePromptRequest(@NotBlank String name, @NotBlank String template, @NotBlank String author) {
    }

    public record ApproveRequest(@NotBlank String approver) {
    }

    public record CompletionRequest(@NotBlank String teamId, @NotBlank String promptName,
                                    @NotNull Map<String, String> variables,
                                    @Min(1) @Max(1024) Integer maxTokens) {
    }

    public record PromptResponse(Long id, String name, int version, String status, String createdBy,
                                 String approvedBy) {
        public static PromptResponse of(PromptVersion p) {
            return new PromptResponse(p.getId(), p.getName(), p.getVersionNo(), p.getStatus().name(),
                    p.getCreatedBy(), p.getApprovedBy());
        }
    }

    public record CompletionResponse(Long callId, String provider, String promptName, int promptVersion,
                                     String output, int promptTokens, int completionTokens, int redactions) {
        public static CompletionResponse of(GatewayCall c) {
            return new CompletionResponse(c.getId(), c.getProvider(), c.getPromptName(), c.getPromptVersion(),
                    c.getOutput(), c.getPromptTokens(), c.getCompletionTokens(), c.getRedactions());
        }
    }

    public record BudgetResponse(String teamId, long limit, long used, long remaining) {
        public static BudgetResponse of(TeamBudget b) {
            return new BudgetResponse(b.getTeamId(), b.getMonthlyTokenLimit(), b.getTokensUsed(), b.remaining());
        }
    }
}
