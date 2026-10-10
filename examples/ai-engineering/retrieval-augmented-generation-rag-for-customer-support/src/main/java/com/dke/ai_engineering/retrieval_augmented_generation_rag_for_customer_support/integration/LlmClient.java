package com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.integration;

/** Port for the chat model. Production: a hosted LLM behind a gateway with timeouts, retries and token budgets. */
public interface LlmClient {
    String complete(String prompt);
}
