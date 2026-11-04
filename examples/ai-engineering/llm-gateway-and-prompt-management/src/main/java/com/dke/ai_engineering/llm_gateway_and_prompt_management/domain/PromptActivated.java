package com.dke.ai_engineering.llm_gateway_and_prompt_management.domain;

/** Stand-in for the Kafka topic "prompt.version.activated". */
public record PromptActivated(Long promptId, String name, int versionNo) {
}
