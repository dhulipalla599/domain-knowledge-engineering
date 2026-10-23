package com.dke.ai_engineering.llm_gateway_and_prompt_management.domain;

import jakarta.persistence.*;

@Entity
public class TeamBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String teamId;

    private long monthlyTokenLimit;
    private long tokensUsed;

    protected TeamBudget() {
    }

    public TeamBudget(String teamId, long monthlyTokenLimit) {
        this.teamId = teamId;
        this.monthlyTokenLimit = monthlyTokenLimit;
    }

    public long remaining() {
        return monthlyTokenLimit - tokensUsed;
    }

    public boolean canAfford(long tokens) {
        return tokens <= remaining();
    }

    public void charge(long tokens) {
        if (tokens < 0) {
            throw new IllegalArgumentException("Cannot charge a negative token count");
        }
        tokensUsed += tokens;
    }

    public String getTeamId() { return teamId; }
    public long getMonthlyTokenLimit() { return monthlyTokenLimit; }
    public long getTokensUsed() { return tokensUsed; }
}
