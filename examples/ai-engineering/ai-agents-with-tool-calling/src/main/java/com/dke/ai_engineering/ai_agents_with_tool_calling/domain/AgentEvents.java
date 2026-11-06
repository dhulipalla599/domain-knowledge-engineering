package com.dke.ai_engineering.ai_agents_with_tool_calling.domain;

/** Domain events. Each stands in for a Kafka topic (named in the comment). */
public final class AgentEvents {

    private AgentEvents() {
    }

    /** topic: agent.run.started */
    public record AgentRunStarted(Long runId, String userId, String goal) {
    }

    /** topic: agent.toolcall.executed */
    public record ToolCallExecuted(Long runId, String tool, ToolCallStatus status) {
    }

    /** topic: agent.approval.requested */
    public record ApprovalRequested(Long runId, String tool, String arguments) {
    }

    /** topic: agent.run.finished */
    public record AgentRunFinished(Long runId, RunStatus status) {
    }
}
