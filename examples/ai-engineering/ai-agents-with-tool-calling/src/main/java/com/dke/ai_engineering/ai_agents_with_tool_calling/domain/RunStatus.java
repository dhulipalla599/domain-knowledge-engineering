package com.dke.ai_engineering.ai_agents_with_tool_calling.domain;

/** Lifecycle of an agent run. Legal moves are enforced by {@link AgentRun}. */
public enum RunStatus {
    RUNNING, AWAITING_APPROVAL, COMPLETED, FAILED;

    public boolean canMoveTo(RunStatus next) {
        return switch (this) {
            case RUNNING -> next == AWAITING_APPROVAL || next == COMPLETED || next == FAILED;
            case AWAITING_APPROVAL -> next == RUNNING || next == FAILED;
            case COMPLETED, FAILED -> false;
        };
    }
}
