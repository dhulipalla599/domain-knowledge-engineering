package com.dke.ai_engineering.ai_agents_with_tool_calling.service;

import java.util.List;
import java.util.Map;

/** Port for the model. The real adapter would call a hosted LLM with a tool-calling API. */
public interface LlmClient {

    Decision decide(String goal, List<Observation> history, List<ToolSpec> tools);

    sealed interface Decision permits ToolRequest, FinalAnswer {
    }

    record ToolRequest(String tool, Map<String, String> arguments) implements Decision {
    }

    record FinalAnswer(String text) implements Decision {
    }

    record ToolSpec(String name, String description, boolean requiresApproval) {
    }

    record Observation(String tool, ToolCallStatusView status, String result) {
    }

    /** Mirrors the stored status without leaking the entity into the model port. */
    enum ToolCallStatusView { EXECUTED, REJECTED, DENIED, ERROR }
}
