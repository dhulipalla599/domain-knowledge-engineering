package com.dke.ai_engineering.ai_agents_with_tool_calling.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AgentRunTest {

    @Test
    void approvalPauseAndResumeIsLegal() {
        AgentRun run = new AgentRun("user-1", "pay CLM-1002");
        run.awaitApproval();
        assertThat(run.getStatus()).isEqualTo(RunStatus.AWAITING_APPROVAL);
        run.resume();
        run.complete("done");
        assertThat(run.getStatus()).isEqualTo(RunStatus.COMPLETED);
        assertThat(run.getFinalAnswer()).isEqualTo("done");
    }

    @Test
    void finishedRunCannotBeReopened() {
        AgentRun run = new AgentRun("user-1", "goal");
        run.fail("step limit");
        assertThatThrownBy(run::resume).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> run.complete("late")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cannotCompleteWhileWaitingForApproval() {
        AgentRun run = new AgentRun("user-1", "goal");
        run.awaitApproval();
        assertThatThrownBy(() -> run.complete("skipped approval")).isInstanceOf(IllegalStateException.class);
    }
}
