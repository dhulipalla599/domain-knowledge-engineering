package com.dke.ai_engineering.ai_agents_with_tool_calling.api;

import com.dke.ai_engineering.ai_agents_with_tool_calling.domain.AgentRun;
import com.dke.ai_engineering.ai_agents_with_tool_calling.domain.RunStatus;
import com.dke.ai_engineering.ai_agents_with_tool_calling.domain.ToolCall;
import java.util.List;

public record AgentRunResponse(Long id, String userId, String goal, RunStatus status, String finalAnswer,
                               String failureReason, List<ToolCallView> toolCalls) {

    public record ToolCallView(int step, String tool, String arguments, String status, String result) {
    }

    static AgentRunResponse of(AgentRun run, List<ToolCall> calls) {
        return new AgentRunResponse(run.getId(), run.getUserId(), run.getGoal(), run.getStatus(),
                run.getFinalAnswer(), run.getFailureReason(),
                calls.stream().map(c -> new ToolCallView(c.getStepNo(), c.getToolName(), c.getArguments(),
                        c.getStatus().name(), c.getResult())).toList());
    }
}
