package com.dke.ai_engineering.ai_agents_with_tool_calling.api;

import com.dke.ai_engineering.ai_agents_with_tool_calling.domain.AgentRun;
import com.dke.ai_engineering.ai_agents_with_tool_calling.service.AgentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent-runs")
public class AgentRunController {

    private final AgentService service;

    public AgentRunController(AgentService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AgentRunResponse start(@Valid @RequestBody AgentRunRequest request) {
        return view(service.start(request.userId(), request.goal()));
    }

    @GetMapping("/{id}")
    public AgentRunResponse get(@PathVariable Long id) {
        return view(service.get(id));
    }

    @PostMapping("/{id}/approval")
    public AgentRunResponse decide(@PathVariable Long id, @Valid @RequestBody ApprovalRequest request) {
        return view(service.decide(id, request.approver(), request.approved()));
    }

    private AgentRunResponse view(AgentRun run) {
        return AgentRunResponse.of(run, service.toolCalls(run.getId()));
    }
}
