package com.dke.ai_engineering.llm_gateway_and_prompt_management.domain;

public enum PromptStatus {
    DRAFT, APPROVED, ACTIVE, RETIRED;

    public boolean canMoveTo(PromptStatus next) {
        return switch (this) {
            case DRAFT -> next == APPROVED || next == RETIRED;
            case APPROVED -> next == ACTIVE || next == RETIRED;
            case ACTIVE -> next == RETIRED;
            case RETIRED -> false;
        };
    }
}
