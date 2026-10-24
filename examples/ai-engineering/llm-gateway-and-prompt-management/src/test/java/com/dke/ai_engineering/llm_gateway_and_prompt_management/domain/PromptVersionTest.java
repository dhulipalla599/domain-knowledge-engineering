package com.dke.ai_engineering.llm_gateway_and_prompt_management.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

class PromptVersionTest {

    private PromptVersion draft() {
        return new PromptVersion("greeting", 1, "Hello {{name}}, welcome to {{bank}}", "author");
    }

    @Test
    void followsTheLegalLifecycle() {
        PromptVersion p = draft();
        p.approve("reviewer");
        p.activate();
        p.retire();
        assertThat(p.getStatus()).isEqualTo(PromptStatus.RETIRED);
        assertThat(p.getApprovedBy()).isEqualTo("reviewer");
    }

    @Test
    void cannotActivateADraft() {
        assertThatThrownBy(() -> draft().activate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DRAFT");
    }

    @Test
    void rendersVariablesAndRejectsMissingOnes() {
        PromptVersion p = draft();
        assertThat(p.render(Map.of("name", "Ana", "bank", "Harbor"))).isEqualTo("Hello Ana, welcome to Harbor");
        assertThatThrownBy(() -> p.render(Map.of("name", "Ana")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Missing variable: bank");
    }

    @Test
    void budgetRefusesWhatItCannotAfford() {
        TeamBudget budget = new TeamBudget("t", 100);
        budget.charge(60);
        assertThat(budget.canAfford(40)).isTrue();
        assertThat(budget.canAfford(41)).isFalse();
    }
}
