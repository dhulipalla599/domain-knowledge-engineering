package com.dke.ai_engineering.llm_gateway_and_prompt_management.integration;

public class ProviderUnavailableException extends RuntimeException {

    public ProviderUnavailableException(String provider) {
        super("Provider unavailable: " + provider);
    }
}
