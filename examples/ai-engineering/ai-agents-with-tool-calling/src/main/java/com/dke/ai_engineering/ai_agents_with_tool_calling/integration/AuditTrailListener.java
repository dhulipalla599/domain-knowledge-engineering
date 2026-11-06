package com.dke.ai_engineering.ai_agents_with_tool_calling.integration;

import com.dke.ai_engineering.ai_agents_with_tool_calling.domain.AgentEvents.AgentRunFinished;
import com.dke.ai_engineering.ai_agents_with_tool_calling.domain.AgentEvents.AgentRunStarted;
import com.dke.ai_engineering.ai_agents_with_tool_calling.domain.AgentEvents.ApprovalRequested;
import com.dke.ai_engineering.ai_agents_with_tool_calling.domain.AgentEvents.ToolCallExecuted;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Stand-in for Kafka consumers that feed the audit store and dashboards. */
@Component
public class AuditTrailListener {

    private static final Logger log = LoggerFactory.getLogger(AuditTrailListener.class);
    private final List<String> entries = new CopyOnWriteArrayList<>();

    @EventListener
    void on(AgentRunStarted e) {
        add("agent.run.started", e.runId() + " user=" + e.userId());
    }

    @EventListener
    void on(ToolCallExecuted e) {
        add("agent.toolcall.executed", e.runId() + " tool=" + e.tool() + " status=" + e.status());
    }

    @EventListener
    void on(ApprovalRequested e) {
        add("agent.approval.requested", e.runId() + " tool=" + e.tool());
    }

    @EventListener
    void on(AgentRunFinished e) {
        add("agent.run.finished", e.runId() + " status=" + e.status());
    }

    public List<String> entries() {
        return List.copyOf(entries);
    }

    private void add(String topic, String body) {
        entries.add(topic + " " + body);
        log.info("[{}] {}", topic, body);
    }
}
