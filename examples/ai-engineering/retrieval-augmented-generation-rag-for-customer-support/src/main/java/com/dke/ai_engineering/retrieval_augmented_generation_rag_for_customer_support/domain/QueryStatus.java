package com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.domain;

/** Lifecycle of a customer question. ESCALATED and RESOLVED are terminal. */
public enum QueryStatus {
    RECEIVED, ANSWERED, ESCALATED, RESOLVED;

    public boolean canMoveTo(QueryStatus next) {
        return switch (this) {
            case RECEIVED -> next == ANSWERED || next == ESCALATED;
            case ANSWERED -> next == RESOLVED || next == ESCALATED;
            case ESCALATED, RESOLVED -> false;
        };
    }
}
