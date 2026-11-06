package com.dke.ai_engineering.ai_agents_with_tool_calling.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AgentRunRequest(@NotBlank String userId, @NotBlank @Size(max = 500) String goal) {
}
