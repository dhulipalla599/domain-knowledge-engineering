package com.dke.ai_engineering.ai_agents_with_tool_calling.api;

import jakarta.validation.constraints.NotBlank;

public record ApprovalRequest(@NotBlank String approver, boolean approved) {
}
