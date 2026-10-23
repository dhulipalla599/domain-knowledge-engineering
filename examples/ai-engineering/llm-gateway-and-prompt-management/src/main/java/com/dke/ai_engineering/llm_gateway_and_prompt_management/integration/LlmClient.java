package com.dke.ai_engineering.llm_gateway_and_prompt_management.integration;

/** Port for a model provider. Production adapters wrap Amazon Bedrock, Azure OpenAI, etc. */
public interface LlmClient {

    String providerName();

    LlmResponse complete(String prompt, int maxTokens);

    /** Rough rule of thumb (about 4 characters per token); real tokenizers differ per model. */
    static int estimateTokens(String text) {
        return (text.length() + 3) / 4;
    }
}
