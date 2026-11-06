package com.dke.ai_engineering.ai_agents_with_tool_calling.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/** One tool invocation requested by the model, with its outcome. */
@Entity
public class ToolCall {

    @Id
    @GeneratedValue
    private Long id;
    private Long runId;
    private int stepNo;
    private String toolName;
    @Column(length = 1000)
    private String arguments;
    @Enumerated(EnumType.STRING)
    private ToolCallStatus status;
    @Column(length = 1000)
    private String result;

    protected ToolCall() {
    }

    public ToolCall(Long runId, int stepNo, String toolName, String arguments, ToolCallStatus status, String result) {
        this.runId = runId;
        this.stepNo = stepNo;
        this.toolName = toolName;
        this.arguments = arguments;
        this.status = status;
        this.result = result;
    }

    public void settle(ToolCallStatus status, String result) {
        this.status = status;
        this.result = result;
    }

    public Long getId() { return id; }
    public Long getRunId() { return runId; }
    public int getStepNo() { return stepNo; }
    public String getToolName() { return toolName; }
    public String getArguments() { return arguments; }
    public ToolCallStatus getStatus() { return status; }
    public String getResult() { return result; }
}
