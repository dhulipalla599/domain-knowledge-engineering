package com.dke.ai_engineering.ai_agents_with_tool_calling.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dke.ai_engineering.ai_agents_with_tool_calling.domain.AgentEvents.AgentRunFinished;
import com.dke.ai_engineering.ai_agents_with_tool_calling.domain.AgentEvents.AgentRunStarted;
import com.dke.ai_engineering.ai_agents_with_tool_calling.domain.AgentEvents.ApprovalRequested;
import com.dke.ai_engineering.ai_agents_with_tool_calling.domain.AgentEvents.ToolCallExecuted;
import com.dke.ai_engineering.ai_agents_with_tool_calling.domain.AgentRun;
import com.dke.ai_engineering.ai_agents_with_tool_calling.domain.RunStatus;
import com.dke.ai_engineering.ai_agents_with_tool_calling.domain.ToolCall;
import com.dke.ai_engineering.ai_agents_with_tool_calling.domain.ToolCallStatus;
import com.dke.ai_engineering.ai_agents_with_tool_calling.repository.AgentRunRepository;
import com.dke.ai_engineering.ai_agents_with_tool_calling.repository.ToolCallRepository;
import com.dke.ai_engineering.ai_agents_with_tool_calling.service.LlmClient.FinalAnswer;
import com.dke.ai_engineering.ai_agents_with_tool_calling.service.LlmClient.Observation;
import com.dke.ai_engineering.ai_agents_with_tool_calling.service.LlmClient.ToolCallStatusView;
import com.dke.ai_engineering.ai_agents_with_tool_calling.service.LlmClient.ToolRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Runs the agent loop: ask the model, validate the tool request, execute or pause, feed back the result. */
@Service
public class AgentService {

    public static final int MAX_STEPS = 4;

    private final AgentRunRepository runs;
    private final ToolCallRepository calls;
    private final ToolRegistry registry;
    private final LlmClient llm;
    private final ApplicationEventPublisher events;
    private final ObjectMapper json;

    public AgentService(AgentRunRepository runs, ToolCallRepository calls, ToolRegistry registry,
                        LlmClient llm, ApplicationEventPublisher events, ObjectMapper json) {
        this.runs = runs;
        this.calls = calls;
        this.registry = registry;
        this.llm = llm;
        this.events = events;
        this.json = json;
    }

    @Transactional
    public AgentRun start(String userId, String goal) {
        AgentRun run = runs.save(new AgentRun(userId, goal));
        events.publishEvent(new AgentRunStarted(run.getId(), userId, goal));
        return drive(run);
    }

    @Transactional
    public AgentRun decide(Long runId, String approver, boolean approved) {
        AgentRun run = get(runId);
        if (run.getStatus() != RunStatus.AWAITING_APPROVAL) {
            throw new IllegalStateException("Run " + runId + " is " + run.getStatus() + ", not awaiting approval");
        }
        if (approver.equals(run.getUserId())) {
            throw new ApprovalNotAllowedException("The requester cannot approve their own agent run");
        }
        ToolCall pending = calls.findFirstByRunIdAndStatus(runId, ToolCallStatus.PENDING_APPROVAL).orElseThrow();
        if (!approved) {
            pending.settle(ToolCallStatus.REJECTED, "Rejected by " + approver);
            events.publishEvent(new ToolCallExecuted(runId, pending.getToolName(), ToolCallStatus.REJECTED));
            run.fail("Rejected by " + approver);
            return finish(run);
        }
        execute(pending, registry.find(pending.getToolName()).orElseThrow());
        run.resume();
        return drive(run);
    }

    @Transactional(readOnly = true)
    public AgentRun get(Long runId) {
        return runs.findById(runId).orElseThrow(() -> new NotFoundException("Agent run " + runId + " not found"));
    }

    @Transactional(readOnly = true)
    public List<ToolCall> toolCalls(Long runId) {
        return calls.findByRunIdOrderByStepNo(runId);
    }

    private AgentRun drive(AgentRun run) {
        while (run.getStatus() == RunStatus.RUNNING) {
            List<ToolCall> history = calls.findByRunIdOrderByStepNo(run.getId());
            var decision = llm.decide(run.getGoal(), observations(history), registry.specs());
            switch (decision) {
                case FinalAnswer answer -> run.complete(answer.text());
                case ToolRequest request -> {
                    if (history.size() >= MAX_STEPS) {
                        run.fail("Step limit of " + MAX_STEPS + " tool calls reached");
                    } else {
                        handle(run, history.size() + 1, request);
                    }
                }
            }
        }
        return run.getStatus() == RunStatus.AWAITING_APPROVAL ? run : finish(run);
    }

    private void handle(AgentRun run, int stepNo, ToolRequest request) {
        String args = write(request.arguments());
        Optional<Tool> tool = registry.find(request.tool());
        if (tool.isEmpty()) {
            record(run, new ToolCall(run.getId(), stepNo, request.tool(), args, ToolCallStatus.DENIED,
                    "Tool '" + request.tool() + "' is not on the allow-list"));
            return;
        }
        ToolCall call = calls.save(new ToolCall(run.getId(), stepNo, request.tool(), args,
                ToolCallStatus.PENDING_APPROVAL, null));
        if (tool.get().requiresApproval()) {
            run.awaitApproval();
            events.publishEvent(new ApprovalRequested(run.getId(), request.tool(), args));
        } else {
            execute(call, tool.get());
        }
    }

    private void execute(ToolCall call, Tool tool) {
        try {
            call.settle(ToolCallStatus.EXECUTED, tool.execute(read(call.getArguments())));
        } catch (IllegalArgumentException e) {
            call.settle(ToolCallStatus.ERROR, e.getMessage());
        }
        events.publishEvent(new ToolCallExecuted(call.getRunId(), call.getToolName(), call.getStatus()));
    }

    private void record(AgentRun run, ToolCall call) {
        calls.save(call);
        events.publishEvent(new ToolCallExecuted(run.getId(), call.getToolName(), call.getStatus()));
    }

    private AgentRun finish(AgentRun run) {
        events.publishEvent(new AgentRunFinished(run.getId(), run.getStatus()));
        return run;
    }

    private List<Observation> observations(List<ToolCall> history) {
        return history.stream()
                .map(c -> new Observation(c.getToolName(), ToolCallStatusView.valueOf(c.getStatus().name()), c.getResult()))
                .toList();
    }

    private String write(Map<String, String> arguments) {
        try {
            return json.writeValueAsString(arguments);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unserialisable tool arguments", e);
        }
    }

    private Map<String, String> read(String arguments) {
        try {
            return json.readValue(arguments, new TypeReference<>() { });
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unreadable tool arguments", e);
        }
    }
}
