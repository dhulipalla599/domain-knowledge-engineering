package com.dke.ai_engineering.ai_agents_with_tool_calling.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** The allow-list: only registered tools can ever run. */
@Component
public class ToolRegistry {

    private final List<Tool> tools;

    public ToolRegistry(List<Tool> tools) {
        this.tools = List.copyOf(tools);
    }

    public Optional<Tool> find(String name) {
        return tools.stream().filter(t -> t.name().equals(name)).findFirst();
    }

    public List<LlmClient.ToolSpec> specs() {
        return tools.stream()
                .map(t -> new LlmClient.ToolSpec(t.name(), t.description(), t.requiresApproval()))
                .toList();
    }
}
