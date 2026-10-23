package com.dke.ai_engineering.llm_gateway_and_prompt_management.domain;

/** Stand-in for the Kafka topic "llm.call.completed". */
public record LlmCallCompleted(Long callId, String teamId, String promptName, int promptVersion,
                               String provider, int totalTokens) {
}
