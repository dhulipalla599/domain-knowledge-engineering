package com.dke.ai_engineering.llm_gateway_and_prompt_management.integration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/** Order matters: the gateway tries clients in this order and fails over on outage. */
@Configuration
public class LlmClientConfig {

    @Bean
    @Order(1)
    LlmClient primaryClient() {
        return new StubLlmClient("primary-stub", true);
    }

    @Bean
    @Order(2)
    LlmClient fallbackClient() {
        return new StubLlmClient("fallback-stub", false);
    }
}
