package com.dke.ai_engineering.llm_gateway_and_prompt_management.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"team_id", "idempotency_key"}))
public class GatewayCall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String teamId;

    @Column(nullable = false)
    private String promptName;

    private int promptVersion;
    private String provider;
    private int promptTokens;
    private int completionTokens;
    private int redactions;
    private String idempotencyKey;

    @Column(length = 4000)
    private String output;

    private Instant createdAt = Instant.now();

    protected GatewayCall() {
    }

    public GatewayCall(String teamId, String promptName, int promptVersion, String provider,
                       int promptTokens, int completionTokens, int redactions,
                       String idempotencyKey, String output) {
        this.teamId = teamId;
        this.promptName = promptName;
        this.promptVersion = promptVersion;
        this.provider = provider;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.redactions = redactions;
        this.idempotencyKey = idempotencyKey;
        this.output = output;
    }

    public int totalTokens() {
        return promptTokens + completionTokens;
    }

    public Long getId() { return id; }
    public String getTeamId() { return teamId; }
    public String getPromptName() { return promptName; }
    public int getPromptVersion() { return promptVersion; }
    public String getProvider() { return provider; }
    public int getPromptTokens() { return promptTokens; }
    public int getCompletionTokens() { return completionTokens; }
    public int getRedactions() { return redactions; }
    public String getOutput() { return output; }
}
