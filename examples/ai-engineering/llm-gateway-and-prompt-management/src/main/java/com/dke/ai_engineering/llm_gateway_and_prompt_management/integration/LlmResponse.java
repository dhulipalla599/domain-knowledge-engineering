package com.dke.ai_engineering.llm_gateway_and_prompt_management.integration;

public record LlmResponse(String provider, String text, int promptTokens, int completionTokens) {
}
