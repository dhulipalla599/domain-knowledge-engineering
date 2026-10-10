package com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.api;

import com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.domain.ArticleStatus;
import com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.domain.KnowledgeArticle;
import com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.domain.QueryStatus;
import com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.domain.SupportQuery;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class SupportDtos {
    private SupportDtos() {}

    public record AskRequest(@NotBlank String customerId, @NotBlank @Size(max = 1000) String question) {}

    public record FeedbackRequest(@NotNull Boolean helpful) {}

    public record QueryResponse(Long id, String customerId, QueryStatus status, String question, String answer,
                                List<Long> citations, Double confidence, String escalationReason) {
        public static QueryResponse from(SupportQuery q) {
            return new QueryResponse(q.getId(), q.getCustomerId(), q.getStatus(), q.getQuestion(), q.getAnswer(),
                    q.getCitedArticleIds(), q.getConfidence(), q.getEscalationReason());
        }
    }

    public record ArticleResponse(Long id, String title, String category, ArticleStatus status) {
        public static ArticleResponse from(KnowledgeArticle a) {
            return new ArticleResponse(a.getId(), a.getTitle(), a.getCategory(), a.getStatus());
        }
    }
}
