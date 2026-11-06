package com.dke.ai_engineering.ai_agents_with_tool_calling.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.time.Instant;

@Entity
public class AgentRun {

    @Id
    @GeneratedValue
    private Long id;
    private String userId;
    @Column(length = 1000)
    private String goal;
    @Enumerated(EnumType.STRING)
    private RunStatus status = RunStatus.RUNNING;
    @Column(length = 2000)
    private String finalAnswer;
    private String failureReason;
    private Instant createdAt = Instant.now();

    protected AgentRun() {
    }

    public AgentRun(String userId, String goal) {
        this.userId = userId;
        this.goal = goal;
    }

    public void awaitApproval() {
        moveTo(RunStatus.AWAITING_APPROVAL);
    }

    public void resume() {
        moveTo(RunStatus.RUNNING);
    }

    public void complete(String answer) {
        moveTo(RunStatus.COMPLETED);
        this.finalAnswer = answer;
    }

    public void fail(String reason) {
        moveTo(RunStatus.FAILED);
        this.failureReason = reason;
    }

    private void moveTo(RunStatus next) {
        if (!status.canMoveTo(next)) {
            throw new IllegalStateException("Run " + id + " cannot move from " + status + " to " + next);
        }
        this.status = next;
    }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public String getGoal() { return goal; }
    public RunStatus getStatus() { return status; }
    public String getFinalAnswer() { return finalAnswer; }
    public String getFailureReason() { return failureReason; }
}
