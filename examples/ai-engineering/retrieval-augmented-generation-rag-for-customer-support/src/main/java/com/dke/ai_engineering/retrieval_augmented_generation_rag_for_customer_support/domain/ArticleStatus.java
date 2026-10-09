package com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.domain;

/** Only PUBLISHED articles may be retrieved; drafts and archived (stale) articles must never ground an answer. */
public enum ArticleStatus { DRAFT, PUBLISHED, ARCHIVED }
