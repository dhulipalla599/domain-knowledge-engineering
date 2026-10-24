package com.dke.ai_engineering.llm_gateway_and_prompt_management.integration;

import java.util.Arrays;
import java.util.stream.Collectors;

/** Deterministic stand-in for a model: echoes the first words of the prompt. */
public class StubLlmClient implements LlmClient {

    public static final String OUTAGE_MARKER = "[simulate-outage]";

    private final String name;
    private final boolean failOnOutageMarker;

    public StubLlmClient(String name, boolean failOnOutageMarker) {
        this.name = name;
        this.failOnOutageMarker = failOnOutageMarker;
    }

    @Override
    public String providerName() {
        return name;
    }

    @Override
    public LlmResponse complete(String prompt, int maxTokens) {
        if (failOnOutageMarker && prompt.contains(OUTAGE_MARKER)) {
            throw new ProviderUnavailableException(name);
        }
        String text = Arrays.stream(prompt.split("\\s+")).limit(12).collect(Collectors.joining(" "));
        text = "[" + name + "] " + text;
        int limit = maxTokens * 4;
        if (text.length() > limit) {
            text = text.substring(0, limit);
        }
        return new LlmResponse(name, text, LlmClient.estimateTokens(prompt), LlmClient.estimateTokens(text));
    }
}
